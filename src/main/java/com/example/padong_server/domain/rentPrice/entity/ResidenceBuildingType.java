package com.example.padong_server.domain.rentPrice.entity;

import java.util.Arrays;

public enum ResidenceBuildingType {
    APARTMENT("아파트"),
    OFFICETEL("오피스텔"),
    ROW_MULTIFAMILY("연립다세대"),
    DETACHED_MULTIFAMILY("단독다가구");

    private final String label;

    ResidenceBuildingType(String label) {
        this.label = label;
    }

    public String code() {
        return name();
    }

    public String label() {
        return label;
    }

    public static ResidenceBuildingType fromLabel(String label) {
        return from(label);
    }

    public static ResidenceBuildingType from(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("건물유형은 비어 있을 수 없습니다.");
        }
        String normalizedLabel = value.trim();
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(normalizedLabel) || type.label.equals(normalizedLabel))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 건물유형입니다: " + value));
    }
}
