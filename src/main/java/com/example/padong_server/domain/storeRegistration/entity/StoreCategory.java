package com.example.padong_server.domain.storeRegistration.entity;

import lombok.Getter;

@Getter
public enum StoreCategory {
    KOREAN("한식"),
    CHINESE("중식"),
    JAPANESE("일식"),
    WESTERN("양식"),
    ASIAN("아시안"),
    BAKERY("베이커리"),
    CAFE_DESSERT("카페·디저트"),
    SNACK("분식"),
    CHICKEN("치킨"),
    PIZZA("피자"),
    BURGER("버거"),
    SALAD("샐러드"),
    BAR("주점·바"),
    GROCERY("식료품"),
    OTHER("기타");

    private final String label;

    StoreCategory(String label) {
        this.label = label;
    }
}
