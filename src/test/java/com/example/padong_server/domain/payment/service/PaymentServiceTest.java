package com.example.padong_server.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.menu.entity.Menu;
import com.example.padong_server.domain.oauth.entity.Role;
import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.payment.client.PortOnePaymentClient;
import com.example.padong_server.domain.payment.dto.OrderMenuRequest;
import com.example.padong_server.domain.payment.dto.PaymentConfirmRequest;
import com.example.padong_server.domain.payment.dto.PaymentPrepareRequest;
import com.example.padong_server.domain.payment.dto.PaymentPrepareResponse;
import com.example.padong_server.domain.payment.dto.PaymentResponse;
import com.example.padong_server.domain.payment.dto.PortOnePaymentResponse;
import com.example.padong_server.domain.payment.entity.GroupOrder;
import com.example.padong_server.domain.payment.entity.GroupOrderMenu;
import com.example.padong_server.domain.payment.entity.GroupOrderStatus;
import com.example.padong_server.domain.payment.entity.Order;
import com.example.padong_server.domain.payment.entity.OrderMenu;
import com.example.padong_server.domain.payment.entity.OrderStatus;
import com.example.padong_server.domain.payment.entity.Payment;
import com.example.padong_server.domain.payment.entity.PaymentStatus;
import com.example.padong_server.domain.payment.repository.GroupOrderMenuRepository;
import com.example.padong_server.domain.payment.repository.GroupOrderRepository;
import com.example.padong_server.domain.payment.repository.OrderMenuRepository;
import com.example.padong_server.domain.payment.repository.OrderRepository;
import com.example.padong_server.domain.payment.repository.PaymentRepository;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private GroupOrderRepository groupOrderRepository;

    @Mock
    private GroupOrderMenuRepository groupOrderMenuRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMenuRepository orderMenuRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PortOnePaymentClient portOnePaymentClient;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                groupOrderRepository,
                groupOrderMenuRepository,
                orderRepository,
                orderMenuRepository,
                paymentRepository,
                portOnePaymentClient
        );
    }

    @Test
    @DisplayName("결제 준비는 유효한 공동구매와 메뉴 요청으로 주문과 결제 정보를 만든다")
    void prepare_createsOrderAndPaymentWhenRequestIsValid() {
        User user = user(1L, "결제테스터");
        Store store = store(3L);
        GroupOrder groupOrder = groupOrder(store, 7L, 5000, 1, 4);
        Menu menu = menu(store, 11L, "제육덮밥", 6000, 5500);
        GroupOrderMenu groupOrderMenu = GroupOrderMenu.builder()
                .id(21L)
                .groupOrder(groupOrder)
                .menu(menu)
                .soldOut(false)
                .build();
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                groupOrder.getId(),
                List.of(new OrderMenuRequest(menu.getId(), 2))
        );

        when(groupOrderRepository.findById(groupOrder.getId())).thenReturn(Optional.of(groupOrder));
        when(groupOrderMenuRepository.findAllByGroupOrderIdAndMenuIdIn(eq(groupOrder.getId()), anyCollection()))
                .thenReturn(List.of(groupOrderMenu));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 31L);
            return order;
        });
        when(orderMenuRepository.save(any(OrderMenu.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentPrepareResponse response = paymentService.prepare(user, request);

        assertThat(response.orderId()).isEqualTo(31L);
        assertThat(response.amount()).isEqualTo(11000L);
        assertThat(response.customerName()).isEqualTo("결제테스터");
        assertThat(response.orderName()).isEqualTo("제육덮밥");
        assertThat(response.isTest()).isTrue();
        assertThat(response.paymentId()).isNotBlank();

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getOrder().getId()).isEqualTo(31L);
        assertThat(savedPayment.getTotalAmount()).isEqualTo(11000L);
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(savedPayment.getPaymentId()).isEqualTo(response.paymentId());
    }

    @Test
    @DisplayName("결제 준비는 주문 메뉴가 비어 있으면 실패한다")
    void prepare_throwsWhenOrderMenusAreEmpty() {
        User user = user(1L, "결제테스터");
        GroupOrder groupOrder = groupOrder(store(3L), 7L, 5000, 1, 4);
        PaymentPrepareRequest request = new PaymentPrepareRequest(groupOrder.getId(), List.of());

        when(groupOrderRepository.findById(groupOrder.getId())).thenReturn(Optional.of(groupOrder));

        assertThatThrownBy(() -> paymentService.prepare(user, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PAYMENT_REQUEST);

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 승인 중 공동구매 인원 증가에 실패하면 결제와 주문을 실패 상태로 돌린다")
    void confirm_marksPaymentFailedWhenParticipantIncreaseFails() {
        User user = user(1L, "결제테스터");
        Store store = store(3L);
        GroupOrder groupOrder = groupOrder(store, 7L, 1000, 1, 3);
        Menu menu = menu(store, 11L, "제육덮밥", 6000, 3000);
        Order order = Order.builder()
                .id(31L)
                .user(user)
                .groupOrder(groupOrder)
                .store(store)
                .totalPrice(3000)
                .orderStatus(OrderStatus.READY)
                .paymentStatus(PaymentStatus.READY)
                .build();
        Payment payment = Payment.builder()
                .id(41L)
                .order(order)
                .paymentId("payment-123")
                .totalAmount(3000L)
                .isTest(true)
                .status(PaymentStatus.READY)
                .build();
        OrderMenu orderMenu = OrderMenu.builder()
                .id(51L)
                .order(order)
                .menu(menu)
                .quantity(1)
                .price(3000)
                .build();
        GroupOrderMenu groupOrderMenu = GroupOrderMenu.builder()
                .id(21L)
                .groupOrder(groupOrder)
                .menu(menu)
                .soldOut(false)
                .build();
        PaymentConfirmRequest request = new PaymentConfirmRequest();
        ReflectionTestUtils.setField(request, "paymentId", "payment-123");

        when(paymentRepository.findByPaymentId("payment-123")).thenReturn(Optional.of(payment));
        when(groupOrderRepository.findById(groupOrder.getId())).thenReturn(Optional.of(groupOrder));
        when(orderMenuRepository.findAllByOrderId(order.getId())).thenReturn(List.of(orderMenu));
        when(groupOrderMenuRepository.findAllByGroupOrderIdAndMenuIdIn(eq(groupOrder.getId()), anyCollection()))
                .thenReturn(List.of(groupOrderMenu));
        when(portOnePaymentClient.getPayment("payment-123")).thenReturn(new PortOnePaymentResponse(
                "payment-123",
                "PAID",
                "tx-123",
                new PortOnePaymentResponse.Amount(3000L, 0L, 0L, 0L, 0L, 3000L, 0L, 0L),
                "pg-123",
                OffsetDateTime.parse("2026-05-08T12:00:00+09:00")
        ));
        when(groupOrderRepository.increaseParticipantsIfAvailable(groupOrder.getId(), order.getTotalPrice()))
                .thenReturn(0);

        PaymentResponse response = paymentService.confirm(user, request);

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.failureReason()).contains("인원");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        verify(groupOrderRepository, never()).decreaseParticipants(any(Long.class), any(Integer.class));
    }

    private User user(Long id, String nickname) {
        User user = User.builder()
                .kakaoId(1000L + id)
                .email("tester" + id + "@example.com")
                .nickname(nickname)
                .role(Role.USER)
                .registered(true)
                .approved(true)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Store store(Long id) {
        return Store.builder()
                .id(id)
                .name("테스트 가게")
                .roadAddress("서울시 송파구 테스트로 1")
                .phoneNumber("010-0000-0000")
                .operatingHours("09:00-18:00")
                .build();
    }

    private GroupOrder groupOrder(Store store, Long id, int minOrderAmount, int currentParticipants, int maxParticipants) {
        return GroupOrder.builder()
                .id(id)
                .store(store)
                .minOrderAmount(minOrderAmount)
                .currentAmount(0)
                .currentParticipants(currentParticipants)
                .maxParticipants(maxParticipants)
                .status(GroupOrderStatus.OPEN)
                .build();
    }

    private Menu menu(Store store, Long id, String name, int originalPrice, int discountPrice) {
        return Menu.builder()
                .id(id)
                .store(store)
                .menuInfo(name)
                .originalPrice(originalPrice)
                .discountPrice(discountPrice)
                .pickupAvailableTime("12:00")
                .recruitmentDeadline("11:00")
                .paymentMethod("CARD")
                .build();
    }
}
