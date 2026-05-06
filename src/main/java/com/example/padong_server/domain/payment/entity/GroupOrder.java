package com.example.padong_server.domain.payment.entity;

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

    private int currentCount;

    private int maxCount;

    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
}
