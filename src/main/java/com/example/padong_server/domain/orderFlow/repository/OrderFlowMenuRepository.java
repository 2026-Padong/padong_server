package com.example.padong_server.domain.orderFlow.repository;

import com.example.padong_server.domain.orderFlow.entity.OrderFlowMenu;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderFlowMenuRepository extends JpaRepository<OrderFlowMenu, Long> {

    List<OrderFlowMenu> findByOrderFlowIdOrderBySortOrderAsc(Long orderFlowId);

    List<OrderFlowMenu> findByOrderFlowIdInOrderByOrderFlowIdAscSortOrderAsc(
            Collection<Long> orderFlowIds);

    /** 결제 검증용 — 모임에 속한 메뉴 중 사용자가 선택한 메뉴 ID 들만 조회. */
    List<OrderFlowMenu> findAllByOrderFlowIdAndMenuIdIn(Long orderFlowId, Collection<Long> menuIds);
}
