package com.example.padong_server.domain.storeRegistration.entity;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.oauth.entity.User;
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
@Table(name = "store_registrations")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_dong_id")
    private AdminDong adminDong;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String roadAddress;

    @Column(length = 255)
    private String detailAddress;

    @Column(nullable = false, length = 30)
    private String phoneNumber;

    @Column(nullable = false, length = 100)
    private String operatingHours;

    @Column(length = 100)
    private String category;

    @Column(length = 1000)
    private String description;

    @Column
    private Integer originalPrice;

    @Column
    private Integer discountPrice;

    @Column
    private Integer maxParticipants;

    @Column
    private Integer currentParticipants;

    @Column(length = 100)
    private String recruitmentDeadline;

    @Column(length = 100)
    private String paymentMethod;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 1000)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    public void update(
            String name,
            String roadAddress,
            String detailAddress,
            String phoneNumber,
            String operatingHours,
            String category,
            String description,
            Integer originalPrice,
            Integer discountPrice,
            Integer maxParticipants,
            Integer currentParticipants,
            String recruitmentDeadline,
            String paymentMethod,
            Double latitude,
            Double longitude,
            String imageUrl,
            AdminDong adminDong
    ) {
        this.name = name;
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
        this.phoneNumber = phoneNumber;
        this.operatingHours = operatingHours;
        this.category = category;
        this.description = description;
        this.originalPrice = originalPrice;
        this.discountPrice = discountPrice;
        this.maxParticipants = maxParticipants;
        this.currentParticipants = currentParticipants;
        this.recruitmentDeadline = recruitmentDeadline;
        this.paymentMethod = paymentMethod;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.adminDong = adminDong;
    }

    public void updateBasicInfo(
            String name,
            String roadAddress,
            String phoneNumber,
            String operatingHours
    ) {
        this.name = name;
        this.roadAddress = roadAddress;
        this.phoneNumber = phoneNumber;
        this.operatingHours = operatingHours;
    }
}
