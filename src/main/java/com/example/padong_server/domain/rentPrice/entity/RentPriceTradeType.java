package com.example.padong_server.domain.rentPrice.entity;

import java.util.Arrays;

public enum RentPriceTradeType {
    SALE("매매"),
    JEONSE("전세"),
    MONTHLY_RENT("월세");

    private final String label;

    RentPriceTradeType(String label) {
        this.label = label;
    }

    public String code() {
        return name();
    }

    public String label() {
        return label;
    }

    public static RentPriceTradeType from(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("거래유형은 비어 있을 수 없습니다.");
        }
        String normalizedValue = value.trim();
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(normalizedValue) || type.label.equals(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 거래유형입니다: " + value));
    }
}
