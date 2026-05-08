package com.example.padong_server.domain.payment.dto;

public record OrderMenuRequest(
        Long menuId,
        int quantity
) {
}
