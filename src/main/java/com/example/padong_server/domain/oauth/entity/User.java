package com.example.padong_server.domain.oauth.entity;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_kakao_id", columnNames = "kakao_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kakao_id", nullable = false, unique = true)
    private Long kakaoId;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(length = 1000)
    private String picture;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_dong_id")
    private AdminDong adminDong;

    @Column(length = 1000)
    private String businessLicenseImageUrl;

    @Column(nullable = false)
    private boolean registered;

    @Column(nullable = false)
    private boolean approved;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private User(
            Long kakaoId,
            String nickname,
            String picture,
            String email,
            Role role,
            AdminDong adminDong,
            String businessLicenseImageUrl,
            boolean registered,
            boolean approved
    ) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.picture = picture;
        this.email = email;
        this.role = role;
        this.adminDong = adminDong;
        this.businessLicenseImageUrl = businessLicenseImageUrl;
        this.registered = registered;
        this.approved = approved;
    }

    public static User createKakaoMember(Long kakaoId, String email, String nickname, String picture) {
        return User.builder()
                .kakaoId(kakaoId)
                .email(email)
                .nickname(nickname)
                .picture(picture)
                .build();
    }

    public void updateProfile(String nickname, String picture, String email, AdminDong adminDong) {
        this.nickname = nickname;
        this.picture = picture;
        this.email = email;
        this.adminDong = adminDong;
    }

    public void updateAdminDong(AdminDong adminDong) {
        this.adminDong = adminDong;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePicture(String picture) {
        this.picture = picture;
    }

    public void completeSignUp(Role role, AdminDong adminDong, String businessLicenseImageUrl) {
        this.role = role;
        this.adminDong = adminDong;
        this.businessLicenseImageUrl = businessLicenseImageUrl;
        this.registered = true;
        this.approved = role == Role.USER;
    }

    public void upgradeToAdmin(String businessLicenseImageUrl, AdminDong adminDong) {
        this.role = Role.ADMIN;
        this.businessLicenseImageUrl = businessLicenseImageUrl;
        if (adminDong != null) {
            this.adminDong = adminDong;
        }
        this.approved = false;
    }

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.nickname = "탈퇴한 사용자";
        this.picture = null;
        this.email = "deleted-" + this.id + "@padong.local";
        this.registered = false;
        this.approved = false;
    }

    public void reactivate() {
        this.deleted = false;
        this.deletedAt = null;
        this.registered = false;
        this.approved = false;
        // role, adminDong, businessLicenseImageUrl 은 후속 completeSignUp 가 덮어씀
    }

    public void approve() {
        this.approved = true;
    }

    public void promoteToAdmin() {
        this.role = Role.ADMIN;
        this.approved = true;
    }
}
