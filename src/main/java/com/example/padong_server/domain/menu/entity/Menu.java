package com.example.padong_server.domain.menu.entity;

import com.example.padong_server.domain.storeRegistration.entity.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "menus")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_registration_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 255)
    private String menuInfo;

    @Column(nullable = false)
    private Integer originalPrice;

    @Column(nullable = false)
    private Integer discountPrice;

    @Column(nullable = false, length = 100)
    private String pickupAvailableTime;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false, length = 100)
    private String recruitmentDeadline;

    @Column(nullable = false, length = 100)
    private String paymentMethod;

    @Column(nullable = false)
    private Integer currentParticipants;

    public void update(
            String menuInfo,
            Integer originalPrice,
            Integer discountPrice,
            String pickupAvailableTime,
            Integer maxParticipants,
            String recruitmentDeadline,
            String paymentMethod,
            Integer currentParticipants
    ) {
        this.menuInfo = menuInfo;
        this.originalPrice = originalPrice;
        this.discountPrice = discountPrice;
        this.pickupAvailableTime = pickupAvailableTime;
        this.maxParticipants = maxParticipants;
        this.recruitmentDeadline = recruitmentDeadline;
        this.paymentMethod = paymentMethod;
        this.currentParticipants = currentParticipants;
    }
}
