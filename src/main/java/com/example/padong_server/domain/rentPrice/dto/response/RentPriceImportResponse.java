package com.example.padong_server.domain.rentPrice.dto.response;

public record RentPriceImportResponse(
        int savedStatCount,
        long sourceRowCount,
        long saleRowCount,
        long jeonseRowCount,
        long monthlyRentRowCount,
        long skippedRowCount
) {
}
