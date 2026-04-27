package com.example.padong_server.domain.hotplace.entity;

import java.util.Arrays;

public enum Category {
    CULTURAL_HERITAGE("\uBB38\uD654\uC720\uC0B0"),
    TOURIST_SPECIAL_ZONE("\uAD00\uAD11\uD2B9\uAD6C"),
    SUBWAY_STATION("\uC9C0\uD558\uCCA0\uC5ED"),
    PARK("\uACF5\uC6D0"),
    PALACE("\uACE0\uAD81"),
    HANOK_VILLAGE("\uD55C\uC625\uB9C8\uC744"),
    RIVER("\uD55C\uAC15\uACF5\uC6D0"),
    MARKET("\uC804\uD1B5\uC2DC\uC7A5"),
    MOUNTAIN("\uC0B0"),
    ETC("\uAE30\uD0C0");

    private final String description;

    Category(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static Category from(String value) {
        return Arrays.stream(values())
                .filter(category -> category.description.equals(value))
                .findFirst()
                .orElse(ETC);
    }
}
