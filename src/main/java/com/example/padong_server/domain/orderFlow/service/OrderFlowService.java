package com.example.padong_server.domain.orderFlow.service;

import com.example.padong_server.domain.menu.dto.MenuResponse;
import com.example.padong_server.domain.menu.entity.Menu;
import com.example.padong_server.domain.menu.repository.MenuRepository;
import com.example.padong_server.domain.orderFlow.dto.OrderFlowCreateRequest;
import com.example.padong_server.domain.orderFlow.dto.OrderFlowParticipantResponse;
import com.example.padong_server.domain.orderFlow.dto.OrderFlowResponse;
import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowMenu;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowStatus;
import com.example.padong_server.domain.orderFlow.repository.OrderFlowMenuRepository;
import com.example.padong_server.domain.orderFlow.repository.OrderFlowRepository;
import com.example.padong_server.domain.payment.entity.Order;
import com.example.padong_server.domain.payment.entity.OrderMenu;
import com.example.padong_server.domain.payment.entity.Payment;
import com.example.padong_server.domain.payment.repository.GroupOrderRepository;
import com.example.padong_server.domain.payment.repository.OrderMenuRepository;
import com.example.padong_server.domain.payment.repository.OrderRepository;
import com.example.padong_server.domain.payment.repository.PaymentRepository;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.repository.StoreRegistrationRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFlowService {

    private static final List<OrderFlowStatus> ACTIVE_STATUSES =
            List.copyOf(EnumSet.of(
                    OrderFlowStatus.PENDING,
                    OrderFlowStatus.WAITING_APPROVAL,
                    OrderFlowStatus.APPROVED,
                    OrderFlowStatus.READY));

    private static final List<OrderFlowStatus> HISTORY_STATUSES =
            List.copyOf(EnumSet.of(OrderFlowStatus.COMPLETED, OrderFlowStatus.REJECTED));

    private final OrderFlowRepository orderFlowRepository;
    private final OrderFlowMenuRepository orderFlowMenuRepository;
    private final MenuRepository menuRepository;
    private final StoreRegistrationRepository storeRepository;
    private final GroupOrderRepository groupOrderRepository;
    private final OrderRepository orderRepository;
    private final OrderMenuRepository orderMenuRepository;
    private final PaymentRepository paymentRepository;

    // ─── 생성 / 취소 ───

    @Transactional
    public OrderFlowResponse createOrderFlow(OrderFlowCreateRequest request) {
        Store store = storeRepository
                .findById(request.storeId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        // 같은 가게에 활성 모임 있으면 차단
        Optional<OrderFlow> active =
                orderFlowRepository.findTopByStoreIdAndStatusInOrderByIdDesc(store.getId(), ACTIVE_STATUSES);
        if (active.isPresent()) {
            throw new CustomException(ErrorCode.ORDER_FLOW_ALREADY_EXISTS);
        }

        List<Menu> menus = menuRepository.findAllById(request.menuIds());
        if (menus.size() != request.menuIds().size()) {
            throw new CustomException(ErrorCode.MENU_NOT_FOUND);
        }
        if (menus.stream().anyMatch(Menu::isSoldOut)) {
            throw new CustomException(ErrorCode.SOLD_OUT_MENU_IN_ORDER_FLOW);
        }

        LocalDateTime start = request.recruitmentStart() != null
                ? request.recruitmentStart()
                : LocalDateTime.now();

        OrderFlow flow = OrderFlow.builder()
                .store(store)
                .menu(menus.get(0))
                .status(OrderFlowStatus.PENDING)
                .recruitmentStart(start)
                .recruitmentDeadline(request.recruitmentDeadline())
                .minOrderPerPerson(request.minOrderPerPerson())
                .paymentMethod(request.paymentMethod())
                .maxParticipants(request.participantTotal())
                .currentParticipants(0)
                .build();
        orderFlowRepository.save(flow);

        for (int i = 0; i < menus.size(); i++) {
            Menu m = menus.get(i);
            orderFlowMenuRepository.save(
                    OrderFlowMenu.builder()
                            .orderFlow(flow)
                            .menu(m)
                            .sortOrder(i)
                            .menuInfoSnapshot(m.getMenuInfo())
                            .priceSnapshot(m.getPrice())
                            .build());
        }
        return toResponse(flow);
    }

    @Transactional
    public OrderFlowResponse cancel(Long orderFlowId, String reason) {
        OrderFlow flow = findOrderFlow(orderFlowId);
        OrderFlowStatus s = flow.getStatus();
        if (!(s == OrderFlowStatus.PENDING || s == OrderFlowStatus.WAITING_APPROVAL)) {
            throw new CustomException(ErrorCode.INVALID_ORDER_FLOW_STATUS);
        }
        flow.cancel(reason);
        return toResponse(flow);
    }

    // ─── 조회 ───

    @Transactional(readOnly = true)
    public OrderFlowResponse getOrderFlowByMenu(Long menuId) {
        if (menuId == null) {
            throw new CustomException(ErrorCode.INVALID_ORDER_FLOW_REQUEST);
        }
        OrderFlow flow = orderFlowRepository
                .findTopByMenuIdOrderByIdDesc(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_FLOW_NOT_FOUND));
        return toResponse(flow);
    }

    @Transactional(readOnly = true)
    public OrderFlowResponse getActiveByStore(Long storeId) {
        OrderFlow flow = orderFlowRepository
                .findTopByStoreIdAndStatusInOrderByIdDesc(storeId, ACTIVE_STATUSES)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_FLOW_NOT_FOUND));
        return toResponse(flow);
    }

    /**
     * 모임 내역 조회. storeId / from / to 모두 optional.
     * - storeId 미입력 시 ownerUserId 의 모든 가게에 걸친 내역
     * - from/to 미입력 시 기간 무제한
     */
    @Transactional(readOnly = true)
    public List<OrderFlowResponse> getHistory(
            Long storeId, Long ownerUserId, LocalDateTime from, LocalDateTime to) {
        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now().plusYears(100);

        List<Long> storeIds;
        if (storeId != null) {
            storeIds = List.of(storeId);
        } else if (ownerUserId != null) {
            storeIds = storeRepository.findByOwnerIdOrderByIdDesc(
                            ownerUserId,
                            org.springframework.data.domain.Pageable.unpaged())
                    .stream()
                    .map(s -> s.getId())
                    .toList();
            if (storeIds.isEmpty()) {
                return List.of();
            }
        } else {
            throw new CustomException(ErrorCode.INVALID_ORDER_FLOW_REQUEST);
        }

        return orderFlowRepository
                .findByStoreIdInAndStatusInAndUpdatedAtBetweenOrderByUpdatedAtDesc(
                        storeIds, HISTORY_STATUSES, effectiveFrom, effectiveTo)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderFlowResponse getById(Long orderFlowId) {
        return toResponse(findOrderFlow(orderFlowId));
    }

    @Transactional(readOnly = true)
    public List<OrderFlowParticipantResponse> getParticipants(Long orderFlowId) {
        OrderFlow flow = findOrderFlow(orderFlowId);
        if (flow.getStore() == null) {
            return List.of();
        }
        // 가게의 가장 최근 GroupOrder (active 또는 종료) → 그 PAID Order 들 = 모임 참여자.
        // 히스토리(COMPLETED) 모임도 참여자 표시 필요.
        var groupOrder = groupOrderRepository
                .findTopByStoreIdOrderByIdDesc(flow.getStore().getId())
                .orElse(null);
        if (groupOrder == null) {
            return List.of();
        }
        List<Order> orders = orderRepository.findPaidByGroupOrderId(groupOrder.getId());
        if (orders.isEmpty()) return List.of();

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        java.util.Map<Long, List<OrderMenu>> itemsByOrderId =
                orderMenuRepository.findAllByOrderIdInWithMenu(orderIds).stream()
                        .collect(java.util.stream.Collectors.groupingBy(om -> om.getOrder().getId()));
        java.util.Map<Long, Payment> paymentByOrderId =
                paymentRepository.findAllByOrderIdIn(orderIds).stream()
                        .collect(java.util.stream.Collectors.toMap(p -> p.getOrder().getId(), p -> p));

        return orders.stream()
                .map(o -> {
                    Payment p = paymentByOrderId.get(o.getId());
                    List<OrderFlowParticipantResponse.Item> items =
                            itemsByOrderId.getOrDefault(o.getId(), List.of()).stream()
                                    .map(om -> OrderFlowParticipantResponse.Item.builder()
                                            .menuId(om.getMenu().getId())
                                            .menuInfo(om.getMenu().getName())
                                            .price(om.getMenu().getPrice() == null
                                                    ? 0
                                                    : om.getMenu().getPrice())
                                            .quantity(om.getQuantity())
                                            .build())
                                    .toList();
                    return OrderFlowParticipantResponse.builder()
                            .userId(o.getUser().getId())
                            .userName(o.getUser().getNickname())
                            .joinedAt(p == null ? null : p.getPaidAt())
                            .items(items)
                            .totalAmount(o.getTotalPrice())
                            .paymentStatus(o.getPaymentStatus().name())
                            .build();
                })
                .toList();
    }

    // ─── 상태 전이 ───

    @Transactional
    public OrderFlowResponse approve(Long orderFlowId) {
        OrderFlow flow = findOrderFlow(orderFlowId);
        validateStatus(flow, OrderFlowStatus.WAITING_APPROVAL);
        flow.approve();
        return toResponse(flow);
    }

    @Transactional
    public OrderFlowResponse reject(Long orderFlowId) {
        OrderFlow flow = findOrderFlow(orderFlowId);
        validateStatus(flow, OrderFlowStatus.WAITING_APPROVAL);
        flow.reject();
        return toResponse(flow);
    }

    @Transactional
    public OrderFlowResponse markReadyForPickup(Long orderFlowId) {
        OrderFlow flow = findOrderFlow(orderFlowId);
        validateStatus(flow, OrderFlowStatus.APPROVED);
        flow.markReadyForPickup();
        return toResponse(flow);
    }

    @Transactional
    public OrderFlowResponse completePickup(Long orderFlowId) {
        OrderFlow flow = findOrderFlow(orderFlowId);
        validateStatus(flow, OrderFlowStatus.READY);
        flow.completePickup();
        return toResponse(flow);
    }

    /** 메뉴가 정원 가득 찼을 때 시스템이 OrderFlow 를 만들어 두는 legacy 자동 흐름. */
    @Transactional
    public void createOrderIfMenuIsFull(Menu menu) {
        if (orderFlowRepository.existsByMenuId(menu.getId())) {
            return;
        }
        OrderFlow flow = OrderFlow.builder()
                .menu(menu)
                .store(menu.getStore())
                .status(OrderFlowStatus.WAITING_APPROVAL)
                .build();
        orderFlowRepository.save(flow);
    }

    // ─── helpers ───

    private OrderFlow findOrderFlow(Long orderFlowId) {
        if (orderFlowId == null) {
            throw new CustomException(ErrorCode.INVALID_ORDER_FLOW_REQUEST);
        }
        return orderFlowRepository
                .findById(orderFlowId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_FLOW_NOT_FOUND));
    }

    private void validateStatus(OrderFlow flow, OrderFlowStatus expected) {
        if (flow.getStatus() != expected) {
            throw new CustomException(ErrorCode.INVALID_ORDER_FLOW_STATUS);
        }
    }

    private OrderFlowResponse toResponse(OrderFlow flow) {
        List<OrderFlowMenu> joins =
                orderFlowMenuRepository.findByOrderFlowIdOrderBySortOrderAsc(flow.getId());

        List<MenuResponse> menuResponses;
        Long firstMenuId;
        String firstMenuInfo;
        Long storeId;
        if (!joins.isEmpty()) {
            // 스냅샷 기준 응답 — 메뉴 사후 수정·삭제 와 무관.
            menuResponses = joins.stream()
                    .map(j -> MenuResponse.builder()
                            .id(j.getMenu().getId())
                            .storeId(flow.getStore() != null ? flow.getStore().getId() : null)
                            .name(j.getMenuInfoSnapshot())
                            .price(j.getPriceSnapshot())
                            .soldOut(j.getMenu().isSoldOut())
                            .build())
                    .toList();
            firstMenuId = joins.get(0).getMenu().getId();
            firstMenuInfo = joins.get(0).getMenuInfoSnapshot();
            storeId = flow.getStore() != null
                    ? flow.getStore().getId()
                    : joins.get(0).getMenu().getStore().getId();
        } else if (flow.getMenu() != null) {
            // legacy 단일 메뉴 — 스냅샷 없으므로 live 값 사용
            Menu m = flow.getMenu();
            menuResponses = List.of(MenuResponse.from(m));
            firstMenuId = m.getId();
            firstMenuInfo = m.getMenuInfo();
            storeId = flow.getStore() != null ? flow.getStore().getId() : m.getStore().getId();
        } else {
            menuResponses = List.of();
            firstMenuId = null;
            firstMenuInfo = null;
            storeId = flow.getStore() != null ? flow.getStore().getId() : null;
        }

        OrderFlowStatus effective = flow.effectiveStatus(LocalDateTime.now());

        return OrderFlowResponse.builder()
                .id(flow.getId())
                .storeId(storeId)
                .menuId(firstMenuId)
                .menuInfo(firstMenuInfo)
                .menus(menuResponses)
                .recruitmentStart(flow.getRecruitmentStart())
                .recruitmentDeadline(flow.getRecruitmentDeadline())
                .minOrderPerPerson(flow.getMinOrderPerPerson())
                .paymentMethod(flow.getPaymentMethod())
                .participantCurrent(flow.getCurrentParticipants())
                .participantTotal(flow.getMaxParticipants())
                .status(effective.name())
                .closingSoon(flow.isClosingSoon())
                .canceledAt(flow.getCanceledAt())
                .canceledReason(flow.getCanceledReason())
                .canApprove(effective == OrderFlowStatus.WAITING_APPROVAL)
                .canReject(effective == OrderFlowStatus.WAITING_APPROVAL)
                .canMarkReadyForPickup(effective == OrderFlowStatus.APPROVED)
                .canCompletePickup(effective == OrderFlowStatus.READY)
                .canCancel(
                        effective == OrderFlowStatus.PENDING
                                || effective == OrderFlowStatus.WAITING_APPROVAL)
                .build();
    }
}
