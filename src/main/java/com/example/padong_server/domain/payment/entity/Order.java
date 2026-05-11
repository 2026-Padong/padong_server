package com.example.padong_server.domain.payment.entity;

import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import jakarta.persistence.*;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    private int totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_order_id", nullable = false)
    private GroupOrder groupOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

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
