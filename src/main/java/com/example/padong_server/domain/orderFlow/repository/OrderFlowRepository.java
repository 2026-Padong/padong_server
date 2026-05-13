package com.example.padong_server.domain.orderFlow.repository;

import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderFlowRepository extends JpaRepository<OrderFlow, Long> {

    /**
     * PENDING 상태에서 정원 미달 시 참여자/누적금액 원자적 증가.
     * 동시성 race 회피 — UPDATE ... WHERE current < max 한 줄로 처리.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update OrderFlow f
            set f.currentParticipants = f.currentParticipants + 1,
                f.currentAmount = f.currentAmount + :amount
            where f.id = :flowId
              and f.currentParticipants < f.maxParticipants
              and f.status = com.example.padong_server.domain.orderFlow.entity.OrderFlowStatus.PENDING
            """)
    int increaseParticipantsIfAvailable(
            @Param("flowId") Long flowId,
            @Param("amount") int amount);

    @Modifying(flushAutomatically = true)
    @Query("""
            update OrderFlow f
            set f.currentParticipants =
                    case when f.currentParticipants > 0 then f.currentParticipants - 1 else 0 end,
                f.currentAmount =
                    case when f.currentAmount >= :amount then f.currentAmount - :amount else 0 end
            where f.id = :flowId
            """)
    int decreaseParticipants(
            @Param("flowId") Long flowId,
            @Param("amount") int amount);

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
