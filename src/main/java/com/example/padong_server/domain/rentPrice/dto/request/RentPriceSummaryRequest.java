package com.example.padong_server.domain.rentPrice.dto.request;

import java.util.List;

public record RentPriceSummaryRequest(
        List<String> adminDongCodes
) {
}
