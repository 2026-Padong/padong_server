package com.example.padong_server.domain.payment.entity;

import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {

    private static final DateTimeFormatter ORDER_NUMBER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, length = 32)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    private int totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_flow_id", nullable = false)
    private OrderFlow orderFlow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /** PK 확정 후 호출. 포맷: yyyyMMdd-{PK 5자리 zero-pad}. 한 번만 발급. */
    public void assignOrderNumber(LocalDate today) {
        if (this.id == null) {
            throw new IllegalStateException("orderNumber 는 PK 확보 후 발급 가능");
        }
        if (this.orderNumber != null) {
            return;
        }
        this.orderNumber = today.format(ORDER_NUMBER_DATE) + "-" + String.format("%05d", this.id);
    }

    public void markPaid() {
        this.paymentStatus = PaymentStatus.PAID;
        this.orderStatus = OrderStatus.PAID;
    }

    public void markCanceled() {
        this.paymentStatus = PaymentStatus.CANCELED;
        this.orderStatus = OrderStatus.CANCELED;
    }

    public void markFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
        this.orderStatus = OrderStatus.FAILED;
    }
}
