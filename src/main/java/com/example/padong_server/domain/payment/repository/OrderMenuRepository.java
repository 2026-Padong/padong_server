package com.example.padong_server.domain.payment.repository;

import com.example.padong_server.domain.payment.entity.OrderMenu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderMenuRepository extends JpaRepository<OrderMenu, Long> {

    List<OrderMenu> findAllByOrderId(Long orderId);
}
