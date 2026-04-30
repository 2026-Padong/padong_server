package com.example.padong_server.domain.picture.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        indexes = {
                @Index(name = "idx_tour_picture_admin_dong_code", columnList = "adminDongCode"),
                @Index(name = "idx_tour_picture_content_id", columnList = "contentId")
        }
)
public class TourPicture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String contentId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String roadAddress;

    @Column(length = 1000)
    private String firstImageUrl;

    @Column(nullable = false)
    private String adminDongName;

    @Column(nullable = false)
    private String adminDongCode;

    @Builder
    private TourPicture(
            String contentId,
            String title,
            String roadAddress,
            String firstImageUrl,
            String adminDongName,
            String adminDongCode
    ) {
        this.contentId = contentId;
        this.title = title;
        this.roadAddress = roadAddress;
        this.firstImageUrl = firstImageUrl;
        this.adminDongName = adminDongName;
        this.adminDongCode = adminDongCode;
    }
}
