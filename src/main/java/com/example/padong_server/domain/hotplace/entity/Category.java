package com.example.padong_server.domain.hotplace.entity;

import java.util.Arrays;

public enum Category {
    CULTURAL_HERITAGE("문화유산"),
    CULTURAL_HERITAGE_COMPLEX("고궁·문화유산"),
    TOURIST_SPECIAL_ZONE("관광특구"),
    SUBWAY_STATION("지하철역"),
    PARK("공원"),
    PALACE("고궁"),
    HANOK_VILLAGE("한옥마을"),
    RIVER("한강공원"),
    MARKET("전통시장"),
    MOUNTAIN("산"),
    COMMERCIAL_DISTRICT("발달상권"),
    DENSE_POPULATION_AREA("인구밀집지역"),
    ETC("기타");

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
