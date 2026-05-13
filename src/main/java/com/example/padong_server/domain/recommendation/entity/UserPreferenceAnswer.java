package com.example.padong_server.domain.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_preference_answers",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_preference_user_id", columnNames = "user_id"),
        indexes = @Index(name = "idx_user_preference_user_id", columnList = "user_id"))
public class UserPreferenceAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer q1;

    @Column(nullable = false)
    private Integer q2;

    @Column(nullable = false)
    private Integer q3;

    @Column(nullable = false)
    private Integer q4;

    @Column(nullable = false)
    private Integer q5;

    @Column(nullable = false)
    private Integer q6;

    @Column(nullable = false)
    private Integer q7;

    @Column(nullable = false)
    private Integer q8;

    @Column(nullable = false)
    private Integer q9;

    @Column(nullable = false)
    private Integer q10;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private UserPreferenceAnswer(
            Long userId,
            Integer q1, Integer q2, Integer q3, Integer q4, Integer q5,
            Integer q6, Integer q7, Integer q8, Integer q9, Integer q10) {
        this.userId = userId;
        this.q1 = q1; this.q2 = q2; this.q3 = q3; this.q4 = q4; this.q5 = q5;
        this.q6 = q6; this.q7 = q7; this.q8 = q8; this.q9 = q9; this.q10 = q10;
    }

    public void updateAnswers(
            Integer q1, Integer q2, Integer q3, Integer q4, Integer q5,
            Integer q6, Integer q7, Integer q8, Integer q9, Integer q10) {
        this.q1 = q1; this.q2 = q2; this.q3 = q3; this.q4 = q4; this.q5 = q5;
        this.q6 = q6; this.q7 = q7; this.q8 = q8; this.q9 = q9; this.q10 = q10;
    }

    @PrePersist
    @PreUpdate
    private void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
