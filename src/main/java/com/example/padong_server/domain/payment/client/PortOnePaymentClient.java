package com.example.padong_server.domain.payment.client;

import com.example.padong_server.domain.payment.dto.PortOneCancelResponse;
import com.example.padong_server.domain.payment.dto.PortOnePaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class PortOnePaymentClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${portone.base-url:https://api.portone.io}")
    private String baseUrl;

    @Value("${portone.api-secret}")
    private String apiSecret;

    public PortOnePaymentResponse getPayment(String paymentId) {
        return webClientBuilder.baseUrl(baseUrl)
                .build()
                .get()
                .uri("/payments/{paymentId}", paymentId)
                .headers(headers -> headers.set("Authorization", getAuthorizationHeader()))
                .retrieve()
                .bodyToMono(PortOnePaymentResponse.class)
                .block();
    }

    public PortOneCancelResponse cancel(String paymentId, String cancelReason) {
        return webClientBuilder.baseUrl(baseUrl)
                .build()
                .post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .headers(headers -> headers.set("Authorization", getAuthorizationHeader()))
                .bodyValue(new CancelPayload(cancelReason))
                .retrieve()
                .bodyToMono(PortOneCancelResponse.class)
                .block();
    }

    public void sync(String paymentId) {
        getPayment(paymentId);
    }

    private String getAuthorizationHeader() {
        return "PortOne " + apiSecret;
    }

    private record CancelPayload(String reason) {
    }
}
