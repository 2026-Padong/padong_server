package com.example.padong_server.domain.dongne.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 행정동 polygon 경계 (GeoJSON Feature JSON 그대로 저장).
 *
 * <p>AdminDong 과 {@code @OneToOne(fetch = LAZY)} — 다른 도메인이 AdminDong 을 join 할 때
 * 13KB blob 이 끌려오지 않도록 분리 테이블 + lazy.
 */
@Entity
@Table(name = "admin_dong_boundary")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminDongBoundary {

    @Id
    @Column(name = "admin_dong_id")
    private Long adminDongId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "admin_dong_id")
    private AdminDong adminDong;

    /** GeoJSON Feature 직렬화 결과. 평균 13KB, 최대 50KB → LONGTEXT 로 충분히. */
    @Lob
    @Column(name = "geometry_json", nullable = false, columnDefinition = "LONGTEXT")
    private String geometryJson;
}
