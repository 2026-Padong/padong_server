package com.example.padong_server.domain.payment.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record PortOnePaymentResponse(
        String id,
        String status,
        String transactionId,
        Amount amount,
        String pgTxId,
        OffsetDateTime paidAt
) {

    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(status);
    }

    public Long paidAmount() {
        return amount == null ? null : amount.paid();
    }

    public Long totalAmount() {
        return amount == null ? null : amount.total();
    }

    public LocalDateTime paidAtLocalDateTime() {
        return paidAt == null ? null : paidAt.toLocalDateTime();
    }

    public record Amount(
            Long total,
            Long taxFree,
            Long vat,
            Long supply,
            Long discount,
            Long paid,
            Long cancelled,
            Long cancelledTaxFree
    ) {
    }
}
