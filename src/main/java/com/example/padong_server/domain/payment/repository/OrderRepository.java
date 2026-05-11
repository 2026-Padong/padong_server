package com.example.padong_server.domain.payment.repository;

import com.example.padong_server.domain.payment.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
