package com.example.padong_server.domain.orderFlow.service;

import com.example.padong_server.domain.menu.entity.Menu;
import com.example.padong_server.domain.menu.repository.MenuRepository;
import com.example.padong_server.domain.orderFlow.dto.OrderFlowResponse;
import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowStatus;
import com.example.padong_server.domain.orderFlow.repository.OrderFlowRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFlowService {

    private final OrderFlowRepository orderFlowRepository;
    private final MenuRepository menuRepository;

    @Transactional
    public void createOrderIfMenuIsFull(Menu menu) {
        if (orderFlowRepository.existsByMenuId(menu.getId())) {
            return;
        }

        OrderFlow orderFlow = OrderFlow.builder()
                .menu(menu)
                .status(OrderFlowStatus.WAITING_APPROVAL)
                .build();
        orderFlowRepository.save(orderFlow);
    }

    @Transactional(readOnly = true)
    public OrderFlowResponse getOrderFlowByMenu(Long menuId) {
        if (menuId == null) {
            throw new CustomException(ErrorCode.INVALID_ORDER_FLOW_REQUEST);
        }

        return toResponse(orderFlowRepository.findTopByMenuIdOrderByIdDesc(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_FLOW_NOT_FOUND)));
    }

    @Transactional
    public OrderFlowResponse approve(Long orderFlowId) {
        OrderFlow orderFlow = findOrderFlow(orderFlowId);
        validateStatus(orderFlow, OrderFlowStatus.WAITING_APPROVAL);
        orderFlow.approve();
        return toResponse(orderFlow);
    }

    @Transactional
    public OrderFlowResponse reject(Long orderFlowId) {
        OrderFlow orderFlow = findOrderFlow(orderFlowId);
        validateStatus(orderFlow, OrderFlowStatus.WAITING_APPROVAL);
        orderFlow.reject();
        return toResponse(orderFlow);
    }

    @Transactional
    public OrderFlowResponse markReadyForPickup(Long orderFlowId) {
        OrderFlow orderFlow = findOrderFlow(orderFlowId);
        validateStatus(orderFlow, OrderFlowStatus.PREPARING);
        orderFlow.markReadyForPickup();
        return toResponse(orderFlow);
    }

    @Transactional
    public OrderFlowResponse completePickup(Long orderFlowId) {
        OrderFlow orderFlow = findOrderFlow(orderFlowId);
        validateStatus(orderFlow, OrderFlowStatus.READY_FOR_PICKUP);
        orderFlow.completePickup();
        return toResponse(orderFlow);
    }

    private OrderFlow findOrderFlow(Long orderFlowId) {
        if (orderFlowId == null) {
            throw new CustomException(ErrorCode.INVALID_ORDER_FLOW_REQUEST);
        }

        return orderFlowRepository.findById(orderFlowId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_FLOW_NOT_FOUND));
    }

    private void validateStatus(OrderFlow orderFlow, OrderFlowStatus expectedStatus) {
        if (orderFlow.getStatus() != expectedStatus) {
            throw new CustomException(ErrorCode.INVALID_ORDER_FLOW_STATUS);
        }
    }

    private OrderFlowResponse toResponse(OrderFlow orderFlow) {
        Menu menu = menuRepository.findById(orderFlow.getMenu().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
        OrderFlowStatus status = orderFlow.getStatus();

        return OrderFlowResponse.builder()
                .id(orderFlow.getId())
                .menuId(menu.getId())
                .storeId(menu.getStore().getId())
                .menuInfo(menu.getMenuInfo())
                .status(status.name())
                .canApprove(status == OrderFlowStatus.WAITING_APPROVAL)
                .canReject(status == OrderFlowStatus.WAITING_APPROVAL)
                .canMarkReadyForPickup(status == OrderFlowStatus.PREPARING)
                .canCompletePickup(status == OrderFlowStatus.READY_FOR_PICKUP)
                .build();
    }
}
