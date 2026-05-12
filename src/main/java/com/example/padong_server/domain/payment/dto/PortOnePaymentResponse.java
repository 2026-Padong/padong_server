package com.example.padong_server.domain.payment.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record PortOnePaymentResponse(
        String id,
        String status,
        String transactionId,
        Amount amount,
        String pgTxId,
        OffsetDateTime paidAt,
        /** PortOne 결제 수단 객체 (e.g. {"type":"PaymentMethodCard", "card": {...}}). */
        java.util.Map<String, Object> method
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

    /** PortOne method.type ("PaymentMethodCard"→"card", "PaymentMethodTransfer"→"transfer" 등). */
    public String resolvedMethod() {
        if (method == null) return null;
        Object type = method.get("type");
        if (!(type instanceof String s)) return null;
        // "PaymentMethodCard" → "card"
        if (s.startsWith("PaymentMethod") && s.length() > "PaymentMethod".length()) {
            String tail = s.substring("PaymentMethod".length());
            return Character.toLowerCase(tail.charAt(0)) + tail.substring(1);
        }
        return s;
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
