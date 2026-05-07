package com.example.padong_server.domain.payment.dto;

import java.time.LocalDateTime;

public record PortOneCancelResponse(
        LocalDateTime canceledAt
) {
}
