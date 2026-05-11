package com.example.padong_server.domain.payment.entity;

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
@Table(name = "group_order")
public class GroupOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int minOrderAmount;

    private int currentAmount;

    private int currentParticipants;

    private int maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupOrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
}
