package com.example.padong_server.domain.orderFlow.entity;

import com.example.padong_server.domain.menu.entity.Menu;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "order_flow_menus",
        indexes = {
            @Index(name = "idx_ofm_order_flow", columnList = "order_flow_id"),
            @Index(name = "idx_ofm_menu", columnList = "menu_id")
        })
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderFlowMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_flow_id", nullable = false)
    private OrderFlow orderFlow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 모임 생성 시점의 메뉴 이름 스냅샷 — 이후 메뉴 수정·삭제 와 무관하게 히스토리 보존. */
    @Column(name = "menu_info_snapshot", nullable = false, length = 255)
    private String menuInfoSnapshot;

    /** 모임 생성 시점의 메뉴 가격 스냅샷 (원). */
    @Column(name = "price_snapshot", nullable = false)
    private Integer priceSnapshot;

    /** 모임 단위 품절 — Menu.soldOut 과 독립적으로 운영. */
    @Column(name = "sold_out", nullable = false)
    @Builder.Default
    private boolean soldOut = false;

    public void markSoldOut(boolean soldOut) {
        this.soldOut = soldOut;
    }
}
