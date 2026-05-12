package com.example.padong_server.domain.payment.repository;

import com.example.padong_server.domain.payment.entity.Order;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** 내 주문 목록 — Order.id DESC (= 생성 시간 역순). Pageable 로 size+1 전달해 hasNext 판별. */
    @Query(
            """
            SELECT o FROM Order o JOIN FETCH o.store
            WHERE o.user.id = :userId
              AND (:cursor IS NULL OR o.id < :cursor)
            ORDER BY o.id DESC
            """)
    List<Order> findMyOrdersCursor(
            @Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    /** 가게 + groupOrder + payment 상태 기반 참여자 조회 (OrderFlow.getParticipants 용). */
    @Query(
            """
            SELECT o FROM Order o
            JOIN FETCH o.user
            WHERE o.groupOrder.id = :groupOrderId
              AND o.paymentStatus = com.example.padong_server.domain.payment.entity.PaymentStatus.PAID
            ORDER BY o.id ASC
            """)
    List<Order> findPaidByGroupOrderId(@Param("groupOrderId") Long groupOrderId);
}
