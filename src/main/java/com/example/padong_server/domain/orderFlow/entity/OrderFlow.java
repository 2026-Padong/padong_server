package com.example.padong_server.domain.orderFlow.entity;

import com.example.padong_server.domain.menu.entity.Menu;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_flows")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderFlowStatus status;

    public void approve() {
        this.status = OrderFlowStatus.PREPARING;
    }

    public void reject() {
        this.status = OrderFlowStatus.REJECTED;
    }

    public void markReadyForPickup() {
        this.status = OrderFlowStatus.READY_FOR_PICKUP;
    }

    public void completePickup() {
        this.status = OrderFlowStatus.PICKUP_COMPLETED;
    }
}
