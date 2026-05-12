package com.example.padong_server.domain.payment.service;

import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowStatus;
import com.example.padong_server.domain.orderFlow.repository.OrderFlowRepository;
import com.example.padong_server.domain.payment.dto.OrderResponse;
import com.example.padong_server.domain.payment.entity.Order;
import com.example.padong_server.domain.payment.entity.OrderMenu;
import com.example.padong_server.domain.payment.entity.Payment;
import com.example.padong_server.domain.payment.repository.OrderMenuRepository;
import com.example.padong_server.domain.payment.repository.OrderRepository;
import com.example.padong_server.domain.payment.repository.PaymentRepository;
import com.example.padong_server.global.CursorPageResponse;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private static final List<OrderFlowStatus> ACTIVE_OR_TERMINAL =
            List.copyOf(EnumSet.allOf(OrderFlowStatus.class));

    private final OrderRepository orderRepository;
    private final OrderMenuRepository orderMenuRepository;
    private final PaymentRepository paymentRepository;
    private final OrderFlowRepository orderFlowRepository;

    @Transactional(readOnly = true)
    public CursorPageResponse<OrderResponse> getMyOrders(Long userId, Long cursor, int size) {
        int cappedSize = Math.min(Math.max(size, 1), 50);

        List<Order> fetched =
                orderRepository.findMyOrdersCursor(
                        userId, cursor, PageRequest.of(0, cappedSize + 1));

        List<Long> orderIds = fetched.stream().map(Order::getId).toList();
        Map<Long, Payment> paymentByOrderId =
                orderIds.isEmpty()
                        ? Map.of()
                        : paymentRepository.findAllByOrderIdIn(orderIds).stream()
                                .collect(Collectors.toMap(p -> p.getOrder().getId(), p -> p));
        Map<Long, List<OrderMenu>> menusByOrderId =
                orderIds.isEmpty()
                        ? Map.of()
                        : orderMenuRepository.findAllByOrderIdInWithMenu(orderIds).stream()
                                .collect(Collectors.groupingBy(om -> om.getOrder().getId()));

        return CursorPageResponse.from(
                fetched,
                cappedSize,
                Order::getId,
                order ->
                        OrderResponse.from(
                                order,
                                paymentByOrderId.get(order.getId()),
                                menusByOrderId.getOrDefault(order.getId(), List.of()),
                                resolveFlowStatus(order)));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        // 본인 주문 아니면 404 (존재 노출 방지)
        if (!order.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        List<OrderMenu> items = orderMenuRepository.findAllByOrderId(orderId);
        return OrderResponse.from(order, payment, items, resolveFlowStatus(order));
    }

    /**
     * Order 의 store 에서 가장 최신 OrderFlow 의 effectiveStatus 를 fulfillment 상태로 노출.
     * 매칭 없으면 null. 추후 OrderFlow.id FK 가 Order 에 붙으면 정확도 향상.
     */
    private String resolveFlowStatus(Order order) {
        if (order.getStore() == null) return null;
        return orderFlowRepository
                .findTopByStoreIdAndStatusInOrderByIdDesc(
                        order.getStore().getId(), ACTIVE_OR_TERMINAL)
                .map(OrderFlow.class::cast)
                .map(f -> f.effectiveStatus(LocalDateTime.now()).name())
                .orElse(null);
    }
}
