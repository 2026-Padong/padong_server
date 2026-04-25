package com.example.padong_server.domain.rentPrice.dto.response;

public record ResidencePriceImportResult(
        int savedStatCount,
        long sourceRowCount,
        long saleRowCount,
        long jeonseRowCount,
        long monthlyRentRowCount,
        long skippedRowCount
) {
}
