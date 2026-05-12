package com.example.padong_server.domain.orderFlow.entity;

import com.example.padong_server.domain.menu.entity.Menu;
import com.example.padong_server.domain.storeRegistration.entity.Store;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "order_flows")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class OrderFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 모임이 속한 가게. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    /** 메뉴 1개 기준 OrderFlow — legacy 호환. 새 모임 흐름에선 OrderFlowMenu 1:N 로 사용. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderFlowStatus status;

    @Column(name = "recruitment_start")
    private LocalDateTime recruitmentStart;

    @Column(name = "recruitment_deadline")
    private LocalDateTime recruitmentDeadline;

    /** 1인당 최소 주문 수량 (모임 정책). */
    @Column(name = "min_order_per_person")
    private Integer minOrderPerPerson;

    /** 모임 결제 방식 (CARD, ONSITE 등 자유 문자열). */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "current_participants")
    @Builder.Default
    private Integer currentParticipants = 0;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "canceled_reason", length = 500)
    private String canceledReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void approve() {
        this.status = OrderFlowStatus.APPROVED;
    }

    public void reject() {
        this.status = OrderFlowStatus.REJECTED;
    }

    public void markReadyForPickup() {
        this.status = OrderFlowStatus.READY;
    }

    public void completePickup() {
        this.status = OrderFlowStatus.COMPLETED;
    }

    /** 모집 중 모임의 취소 — REJECTED 로 통합 + 사유·시각 보존. */
    public void cancel(String reason) {
        this.status = OrderFlowStatus.REJECTED;
        this.canceledAt = LocalDateTime.now();
        this.canceledReason = reason;
    }

    public void incrementParticipants() {
        this.currentParticipants = (this.currentParticipants == null ? 0 : this.currentParticipants) + 1;
    }

    /**
     * PENDING 상태에서 정원/마감 도달 시 WAITING_APPROVAL 로 자동 승격.
     * 응답 시점 동적 계산용 — DB 변경 없음.
     */
    public OrderFlowStatus effectiveStatus(LocalDateTime now) {
        if (status != OrderFlowStatus.PENDING) {
            return status;
        }
        if (maxParticipants != null
                && currentParticipants != null
                && currentParticipants >= maxParticipants) {
            return OrderFlowStatus.WAITING_APPROVAL;
        }
        if (recruitmentDeadline != null && !recruitmentDeadline.isAfter(now)) {
            return OrderFlowStatus.WAITING_APPROVAL;
        }
        return OrderFlowStatus.PENDING;
    }

    /** 마감 임박 — 정원 1자리 남았을 때. PENDING 상태에서만 의미 있음. */
    public boolean isClosingSoon() {
        if (status != OrderFlowStatus.PENDING
                || maxParticipants == null
                || currentParticipants == null) {
            return false;
        }
        return currentParticipants == maxParticipants - 1;
    }
}
