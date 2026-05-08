package com.example.padong_server.domain.rentPrice.entity;

import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;

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

    public static RentPriceTradeType fromNullable(String value) {
        String normalizedValue = trimToNull(value);
        return normalizedValue == null ? null : from(normalizedValue);
    }

    public static RentPriceTradeType fromNullableOrDefault(
            String value, RentPriceTradeType defaultType) {
        RentPriceTradeType tradeType = fromNullable(value);
        return tradeType == null ? defaultType : tradeType;
    }

    public static RentPriceTradeType from(String value) {
        String normalizedValue = trimToNull(value);
        Preconditions.validate(
                normalizedValue != null, ErrorCode.RENT_PRICE_TRADE_TYPE_REQUIRED);
        return Arrays.stream(values())
                .filter(
                        type ->
                                type.name().equalsIgnoreCase(normalizedValue)
                                        || type.label.equals(normalizedValue))
                .findFirst()
                .orElseThrow(
                        () ->
                                new CustomException(
                                        ErrorCode.INVALID_RENT_PRICE_TRADE_TYPE,
                                        ErrorCode.INVALID_RENT_PRICE_TRADE_TYPE.getMessage()
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
