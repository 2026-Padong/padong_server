package com.example.padong_server.domain.rentPrice.entity;

import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;

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

    public static ResidenceBuildingType fromNullable(String value) {
        String normalizedValue = trimToNull(value);
        return normalizedValue == null ? null : from(normalizedValue);
    }

    public static ResidenceBuildingType fromNullableOrDefault(
            String value, ResidenceBuildingType defaultType) {
        ResidenceBuildingType buildingType = fromNullable(value);
        return buildingType == null ? defaultType : buildingType;
    }

    public static ResidenceBuildingType from(String value) {
        String normalizedLabel = trimToNull(value);
        Preconditions.validate(
                normalizedLabel != null, ErrorCode.RESIDENCE_BUILDING_TYPE_REQUIRED);
        return Arrays.stream(values())
                .filter(
                        type ->
                                type.name().equalsIgnoreCase(normalizedLabel)
                                        || type.label.equals(normalizedLabel))
                .findFirst()
                .orElseThrow(
                        () ->
                                new CustomException(
                                        ErrorCode.INVALID_RESIDENCE_BUILDING_TYPE,
                                        ErrorCode.INVALID_RESIDENCE_BUILDING_TYPE.getMessage()
                                                + ": "
                                                + value));
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
