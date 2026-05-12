package com.example.padong_server.domain.payment.repository;

import com.example.padong_server.domain.payment.entity.OrderMenu;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderMenuRepository extends JpaRepository<OrderMenu, Long> {

    List<OrderMenu> findAllByOrderId(Long orderId);

    /** 배치 fetch — menu 까지 JOIN FETCH 해서 N+1 방지. */
    @Query(
            """
            SELECT om FROM OrderMenu om JOIN FETCH om.menu
            WHERE om.order.id IN :orderIds
            """)
    List<OrderMenu> findAllByOrderIdInWithMenu(Collection<Long> orderIds);
}
