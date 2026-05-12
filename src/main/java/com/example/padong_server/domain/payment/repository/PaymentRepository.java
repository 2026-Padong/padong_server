package com.example.padong_server.domain.payment.repository;

import com.example.padong_server.domain.payment.entity.Payment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findAllByOrderIdIn(Collection<Long> orderIds);
}
