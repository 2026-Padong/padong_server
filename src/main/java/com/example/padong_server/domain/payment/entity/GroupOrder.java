package com.example.padong_server.domain.payment.entity;

import com.example.padong_server.domain.storeRegistration.entity.ShopStatus;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;
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

    private static final Duration CLOSING_THRESHOLD = Duration.ofHours(24);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int minOrderAmount;

    private int currentAmount;

    private int currentParticipants;

    private int maxParticipants;

    @Column(name = "recruitment_deadline")
    private LocalDateTime recruitmentDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupOrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    public ShopStatus computeStatus(LocalDateTime now) {
        if (status == GroupOrderStatus.CLOSED || currentParticipants >= maxParticipants) {
            return ShopStatus.CLOSED;
        }
        if (recruitmentDeadline != null) {
            if (!recruitmentDeadline.isAfter(now)) {
                return ShopStatus.CLOSED;
            }
            if (Duration.between(now, recruitmentDeadline).compareTo(CLOSING_THRESHOLD) <= 0) {
                return ShopStatus.CLOSING;
            }
        }
        return ShopStatus.RECRUITING;
    }
}
