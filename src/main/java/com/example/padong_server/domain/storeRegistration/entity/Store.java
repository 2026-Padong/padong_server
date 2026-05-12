package com.example.padong_server.domain.storeRegistration.entity;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.oauth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "store_registrations")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at IS NULL")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_dong_id", nullable = false)
    private AdminDong adminDong;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 30)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StoreCategory category;

    @Column(length = 1000)
    private String description;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /** 영업 요일 비트마스크. bit0=MON ... bit6=SUN. 0~127. */
    @Column(name = "weekday_mask", nullable = false)
    @Builder.Default
    private int weekdayMask = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 부분 수정 — 비어있지 않은 필드만 갱신. PATCH 시맨틱. */
    public void patch(
            String name,
            StoreCategory category,
            String address,
            String phoneNumber,
            String description,
            LocalTime openTime,
            LocalTime closeTime,
            Integer weekdayMask,
            Double latitude,
            Double longitude,
            AdminDong adminDong) {
        if (name != null) this.name = name;
        if (category != null) this.category = category;
        if (address != null) this.address = address;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (description != null) this.description = description;
        if (openTime != null) this.openTime = openTime;
        if (closeTime != null) this.closeTime = closeTime;
        if (weekdayMask != null) this.weekdayMask = weekdayMask;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
        if (adminDong != null) this.adminDong = adminDong;
    }

    public void changeThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
