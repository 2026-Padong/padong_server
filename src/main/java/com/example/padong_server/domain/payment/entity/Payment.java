package com.example.padong_server.domain.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_payment_id", columnNames = "payment_id")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 우리 서비스 주문
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 포트원 결제 아이디
    @Column(name = "payment_id", nullable = false, unique = true)
    private String paymentId;

    // 결제 시도 아이디
    @Column(name = "tx_id")
    private String txId;

    // PG사 결제 아이디
    @Column(name = "pg_tx_id")
    private String pgTxId;

    // 상점 아이디
    @Column(name = "store_id")
    private String storeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private Long totalAmount;


    @Column(nullable = false)
    private Boolean isTest;

    private LocalDateTime paidAt;

    private LocalDateTime failedAt;

    private LocalDateTime canceledAt;

    private String failureReason;

    public void markPaid(String pgTxId, LocalDateTime paidAt) {
        this.status = PaymentStatus.PAID;
        this.pgTxId = pgTxId;
        this.paidAt = paidAt;
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.failedAt = LocalDateTime.now();
    }

    public void markCanceled() {
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }
}