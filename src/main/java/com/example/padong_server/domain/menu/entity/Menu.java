package com.example.padong_server.domain.menu.entity;

import com.example.padong_server.domain.storeRegistration.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Table(name = "menus")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "메뉴 PK", example = "7")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_registration_id", nullable = false)
    private Store store;

    @Schema(description = "메뉴 이름", example = "통밀 식빵")
    @Column(name = "menu_info", nullable = false, length = 255)
    private String name;

    @Schema(description = "판매 가격 (원)", example = "5500")
    @Column(nullable = false)
    private Integer price;

    @Schema(description = "품절 여부", example = "false")
    @Column(name = "sold_out", nullable = false)
    @Builder.Default
    private boolean soldOut = false;

    public void update(String name, Integer price) {
        this.name = name;
        this.price = price;
    }

    public void changeSoldOut(boolean soldOut) {
        this.soldOut = soldOut;
    }

    /** 옛 호출처 호환 — `getMenuInfo()` 가 `getName()` 과 동일. */
    public String getMenuInfo() {
        return name;
    }
}
