package com.example.padong_server.domain.orderFlow.repository;

import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderFlowRepository extends JpaRepository<OrderFlow, Long> {

    Optional<OrderFlow> findTopByMenuIdOrderByIdDesc(Long menuId);

    boolean existsByMenuId(Long menuId);
}
