package com.example.padong_server.domain.payment.service;

import com.example.padong_server.domain.oauth.entity.Role;
import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.payment.client.PortOnePaymentClient;
import com.example.padong_server.domain.payment.dto.OrderMenuRequest;
import com.example.padong_server.domain.payment.dto.PaymentCancelRequest;
import com.example.padong_server.domain.payment.dto.PaymentConfirmRequest;
import com.example.padong_server.domain.payment.dto.PaymentPrepareRequest;
import com.example.padong_server.domain.payment.dto.PaymentPrepareResponse;
import com.example.padong_server.domain.payment.dto.PaymentResponse;
import com.example.padong_server.domain.payment.dto.PortOneCancelResponse;
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
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final boolean DEFAULT_TEST_PAYMENT = true;

    private final GroupOrderRepository groupOrderRepository;
    private final GroupOrderMenuRepository groupOrderMenuRepository;
    private final OrderRepository orderRepository;
    private final OrderMenuRepository orderMenuRepository;
    private final PaymentRepository paymentRepository;
    private final PortOnePaymentClient portOnePaymentClient;

    @Transactional
    public PaymentPrepareResponse prepare(User user, PaymentPrepareRequest request) {
        GroupOrder groupOrder = findOpenGroupOrder(request.groupOrderId());
        validateAvailableParticipants(groupOrder);

        Map<Long, Integer> quantities = aggregateQuantities(request.orderMenus());
        List<GroupOrderMenu> groupOrderMenus = groupOrderMenuRepository.findAllByGroupOrderIdAndMenuIdIn(
                groupOrder.getId(),
                quantities.keySet()
        );
        validateAllMenusIncluded(quantities, groupOrderMenus);

        int totalAmount = calculateAndValidateMenus(groupOrderMenus, quantities);
        validateMinOrderAmount(groupOrder, totalAmount);

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .groupOrder(groupOrder)
                .store(groupOrder.getStore())
                .totalPrice(totalAmount)
                .orderStatus(OrderStatus.READY)
                .paymentStatus(PaymentStatus.READY)
                .build());

        for (GroupOrderMenu groupOrderMenu : groupOrderMenus) {
            int quantity = quantities.get(groupOrderMenu.getMenu().getId());
            orderMenuRepository.save(OrderMenu.builder()
                    .order(order)
                    .menu(groupOrderMenu.getMenu())
                    .quantity(quantity)
                    .price(groupOrderMenu.getMenu().getDiscountPrice() * quantity)
                    .build());
        }

        String paymentId = UUID.randomUUID().toString();
        paymentRepository.save(Payment.builder()
                .order(order)
                .paymentId(paymentId)
                .totalAmount((long) totalAmount)
                .isTest(DEFAULT_TEST_PAYMENT)
                .status(PaymentStatus.READY)
                .build());

        return new PaymentPrepareResponse(
                order.getId(),
                paymentId,
                (long) totalAmount,
                buildOrderName(groupOrderMenus, quantities),
                user.getNickname(),
                DEFAULT_TEST_PAYMENT
        );
    }

    @Transactional
    public PaymentResponse confirm(User user, PaymentConfirmRequest request) {
        Payment payment = findPayment(request.getPaymentId());
        validatePaymentOwner(payment, user);
        validateReadyPayment(payment);

        Order order = payment.getOrder();

        GroupOrder groupOrder = findOpenGroupOrder(order.getGroupOrder().getId());
        validateAvailableParticipants(groupOrder);
        validateOrderMenus(order);

        PortOnePaymentResponse portOnePayment = portOnePaymentClient.getPayment(payment.getPaymentId());
        validatePortOnePayment(payment, portOnePayment);

        int updatedRows = groupOrderRepository.increaseParticipantsIfAvailable(
                groupOrder.getId(),
                order.getTotalPrice()
        );
        if (updatedRows != 1) {
            payment.markFailed("공구 참여 가능 인원이 초과되었습니다.");
            order.markFailed();
            return PaymentResponse.from(payment);
        }

        try {
            payment.updateConfirmRequest(portOnePayment.transactionId());
            payment.markPaid(portOnePayment.pgTxId(), portOnePayment.paidAtLocalDateTime());
            order.markPaid();
            return PaymentResponse.from(payment);
        } catch (RuntimeException exception) {
            groupOrderRepository.decreaseParticipants(groupOrder.getId(), order.getTotalPrice());
            payment.markFailed(exception.getMessage());
            order.markFailed();
            return PaymentResponse.from(payment);
        }
    }

    @Transactional
    public PaymentResponse cancel(User user, String paymentId, PaymentCancelRequest request) {
        Payment payment = findPayment(paymentId);
        validatePaymentOwnerOrAdmin(payment, user);

        if (!payment.getStatus().canCancel()) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        Order order = payment.getOrder();
        PortOneCancelResponse cancelResponse = portOnePaymentClient.cancel(paymentId, request.cancelReason());

        payment.markCanceled(cancelResponse.canceledAt());
        order.markCanceled();
        groupOrderRepository.decreaseParticipants(order.getGroupOrder().getId(), order.getTotalPrice());

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(User user, String paymentId) {
        Payment payment = findPayment(paymentId);
        validatePaymentOwnerOrAdmin(payment, user);
        portOnePaymentClient.sync(paymentId);
        return PaymentResponse.from(payment);
    }

    private GroupOrder findOpenGroupOrder(Long groupOrderId) {
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_ORDER_NOT_FOUND));
        if (groupOrder.getStatus() != GroupOrderStatus.OPEN) {
            throw new CustomException(ErrorCode.INVALID_GROUP_ORDER_STATUS);
        }
        return groupOrder;
    }

    private Payment findPayment(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private void validateAvailableParticipants(GroupOrder groupOrder) {
        if (groupOrder.getCurrentParticipants() >= groupOrder.getMaxParticipants()) {
            throw new CustomException(ErrorCode.GROUP_ORDER_FULL);
        }
    }

    private Map<Long, Integer> aggregateQuantities(List<OrderMenuRequest> orderMenus) {
        if (CollectionUtils.isEmpty(orderMenus)) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_REQUEST, "주문 메뉴는 비어 있을 수 없습니다.");
        }

        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderMenuRequest orderMenu : orderMenus) {
            if (orderMenu.menuId() == null || orderMenu.quantity() <= 0) {
                throw new CustomException(ErrorCode.INVALID_PAYMENT_REQUEST, "메뉴와 수량을 확인해주세요.");
            }
            quantities.merge(orderMenu.menuId(), orderMenu.quantity(), Integer::sum);
        }
        return quantities;
    }

    private void validateAllMenusIncluded(Map<Long, Integer> quantities, List<GroupOrderMenu> groupOrderMenus) {
        if (groupOrderMenus.size() != quantities.size()) {
            throw new CustomException(ErrorCode.INVALID_GROUP_ORDER_MENU);
        }
    }

    private int calculateAndValidateMenus(List<GroupOrderMenu> groupOrderMenus, Map<Long, Integer> quantities) {
        int totalAmount = 0;
        for (GroupOrderMenu groupOrderMenu : groupOrderMenus) {
            if (groupOrderMenu.isSoldOut()) {
                throw new CustomException(ErrorCode.SOLD_OUT_MENU);
            }
            int quantity = quantities.get(groupOrderMenu.getMenu().getId());
            totalAmount += groupOrderMenu.getMenu().getDiscountPrice() * quantity;
        }
        return totalAmount;
    }

    private void validateMinOrderAmount(GroupOrder groupOrder, int totalAmount) {
        if (totalAmount < groupOrder.getMinOrderAmount()) {
            throw new CustomException(ErrorCode.MIN_ORDER_AMOUNT_NOT_MET);
        }
    }

    private void validateOrderMenus(Order order) {
        Map<Long, Integer> quantities = orderMenuRepository.findAllByOrderId(order.getId()).stream()
                .collect(Collectors.toMap(orderMenu -> orderMenu.getMenu().getId(), OrderMenu::getQuantity));
        List<GroupOrderMenu> groupOrderMenus = groupOrderMenuRepository.findAllByGroupOrderIdAndMenuIdIn(
                order.getGroupOrder().getId(),
                quantities.keySet()
        );
        validateAllMenusIncluded(quantities, groupOrderMenus);

        int recalculatedAmount = calculateAndValidateMenus(groupOrderMenus, quantities);
        validateMinOrderAmount(order.getGroupOrder(), recalculatedAmount);
        if (recalculatedAmount != order.getTotalPrice()) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }
    }

    private void validateReadyPayment(Payment payment) {
        if (payment.getStatus() != PaymentStatus.READY) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }

    private void validatePortOnePayment(Payment payment, PortOnePaymentResponse portOnePayment) {
        if (portOnePayment == null || !portOnePayment.isPaid()) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS, "포트원 결제가 완료되지 않았습니다.");
        }

        if (!payment.getPaymentId().equals(portOnePayment.id())) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS, "포트원 결제 ID가 일치하지 않습니다.");
        }

        Long totalAmount = portOnePayment.totalAmount();
        Long paidAmount = portOnePayment.paidAmount();
        if (totalAmount == null
                || paidAmount == null
                || !totalAmount.equals(payment.getTotalAmount())
                || !paidAmount.equals(payment.getTotalAmount())) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        if (portOnePayment.paidAtLocalDateTime() == null) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS, "포트원 결제 완료 시간이 없습니다.");
        }
    }

    private void validatePaymentOwner(Payment payment, User user) {
        if (!payment.getOrder().getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.PAYMENT_ACCESS_DENIED);
        }
    }

    private void validatePaymentOwnerOrAdmin(Payment payment, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        validatePaymentOwner(payment, user);
    }

    private String buildOrderName(List<GroupOrderMenu> groupOrderMenus, Map<Long, Integer> quantities) {
        Map<Long, GroupOrderMenu> menuById = groupOrderMenus.stream()
                .collect(Collectors.toMap(groupOrderMenu -> groupOrderMenu.getMenu().getId(), Function.identity()));
        Long firstMenuId = quantities.keySet().iterator().next();
        String firstMenuName = menuById.get(firstMenuId).getMenu().getMenuInfo();
        if (quantities.size() == 1) {
            return firstMenuName;
        }
        return firstMenuName + " 외 " + (quantities.size() - 1) + "건";
    }
}
