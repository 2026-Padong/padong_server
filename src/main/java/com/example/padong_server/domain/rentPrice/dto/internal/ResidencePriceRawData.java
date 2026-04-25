package com.example.padong_server.domain.rentPrice.dto.internal;

import java.math.BigDecimal;
import java.util.List;

public record ResidencePriceRawData(
        List<SaleRow> saleRows,
        List<RentRow> rentRows,
        long sourceRowCount,
        long skippedRowCount
) {

    public record SaleRow(
            String legalDongCode,
            String buildingType,
            Long salePrice,
            BigDecimal area
    ) {
    }

    public record RentRow(
            String legalDongCode,
            String buildingType,
            RentType rentType,
            Long deposit,
            Long monthlyRent,
            BigDecimal area
    ) {
    }

    public enum RentType {
        JEONSE,
        MONTHLY_RENT
    }
}
