package com.example.padong_server.domain.orderFlow.repository;

import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderFlowRepository extends JpaRepository<OrderFlow, Long> {

    Optional<OrderFlow> findTopByMenuIdOrderByIdDesc(Long menuId);

    boolean existsByMenuId(Long menuId);

    /** 가게의 현재 활성 모임 (RECRUITING / CLOSING / PENDING_FULL / WAITING_APPROVAL / PREPARING / READY_FOR_PICKUP) 중 최신 1개. */
    Optional<OrderFlow> findTopByStoreIdAndStatusInOrderByIdDesc(
            Long storeId, List<OrderFlowStatus> statuses);

    /** 가게의 종결된 모임 (PICKUP_COMPLETED / REJECTED / CANCELED) 내역. */
    List<OrderFlow> findByStoreIdAndStatusInAndUpdatedAtBetweenOrderByUpdatedAtDesc(
            Long storeId,
            List<OrderFlowStatus> statuses,
            LocalDateTime from,
            LocalDateTime to);

    /** 여러 가게에 걸친 내역. owner 의 전 가게 fallback. */
    List<OrderFlow> findByStoreIdInAndStatusInAndUpdatedAtBetweenOrderByUpdatedAtDesc(
            java.util.Collection<Long> storeIds,
            List<OrderFlowStatus> statuses,
            LocalDateTime from,
            LocalDateTime to);

    /** 메뉴 ID 의 진행 중 모임 (sold-out 토글 시 검증용). */
    boolean existsByMenuIdAndStatusIn(Long menuId, List<OrderFlowStatus> statuses);

    /** OrderFlowMenu 조인 통한 진행 중 모임 보유 여부 검증 — 메뉴 ID 가 묶여있으면 true. */
    @org.springframework.data.jpa.repository.Query(
            """
            SELECT CASE WHEN COUNT(ofm) > 0 THEN TRUE ELSE FALSE END
            FROM OrderFlowMenu ofm
            WHERE ofm.menu.id = :menuId
              AND ofm.orderFlow.status IN :statuses
            """)
    boolean existsActiveOrderFlowContainingMenu(
            @org.springframework.data.repository.query.Param("menuId") Long menuId,
            @org.springframework.data.repository.query.Param("statuses")
                    java.util.Collection<OrderFlowStatus> statuses);
}
