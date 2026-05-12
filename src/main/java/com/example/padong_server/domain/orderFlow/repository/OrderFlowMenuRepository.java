package com.example.padong_server.domain.orderFlow.repository;

import com.example.padong_server.domain.orderFlow.entity.OrderFlowMenu;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderFlowMenuRepository extends JpaRepository<OrderFlowMenu, Long> {

    List<OrderFlowMenu> findByOrderFlowIdOrderBySortOrderAsc(Long orderFlowId);

    List<OrderFlowMenu> findByOrderFlowIdInOrderByOrderFlowIdAscSortOrderAsc(
            Collection<Long> orderFlowIds);
}
