package com.example.padong_server.domain.news.entity;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_news_article_admin_dong_originallink", columnNames = {"admin_dong_id", "originallink"})
        }
)
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_dong_id", nullable = false)
    private AdminDong adminDong;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 1000)
    private String originallink;

    @Column(length = 1000)
    private String thumbnail;

    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    private NewsArticle(AdminDong adminDong, String title, String description, String originallink, String thumbnail, LocalDateTime fetchedAt) {
        this.adminDong = adminDong;
        this.title = title;
        this.description = description;
        this.originallink = originallink;
        this.thumbnail = thumbnail;
        this.fetchedAt = fetchedAt;
    }

    public static NewsArticle of(
            AdminDong adminDong,
            String title,
            String description,
            String originallink,
            String thumbnail,
            LocalDateTime fetchedAt
    ) {
        return new NewsArticle(adminDong, title, description, originallink, thumbnail, fetchedAt);
    }
}
