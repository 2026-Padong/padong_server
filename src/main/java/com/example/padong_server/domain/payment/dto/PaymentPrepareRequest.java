package com.example.padong_server.domain.payment.dto;

import java.util.List;

public record PaymentPrepareRequest(
        Long groupOrderId,
        List<OrderMenuRequest> orderMenus
) {
}
