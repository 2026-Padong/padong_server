package com.example.padong_server.domain.recommendation.client;

import com.example.padong_server.domain.recommendation.dto.AiDongneRecommendationResponse;
import com.example.padong_server.domain.recommendation.dto.PersonalRecommendationRequest;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

@Component
@RequiredArgsConstructor
public class AiRecommendationClient {

    private static final String PERSONAL_RECOMMENDATIONS_PATH = "/api/v1/dongne/recommendations";

    private final WebClient.Builder webClientBuilder;

    @Value("${ai.server.url:}")
    private String aiServerUrl;

    public AiDongneRecommendationResponse getPersonalRecommendations(PersonalRecommendationRequest request) {
        if (!StringUtils.hasText(aiServerUrl)) {
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_API_CALL_FAILED, "AI 서버 URL 설정이 없습니다.");
        }

        try {
            return webClientBuilder.baseUrl(aiServerUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> buildPersonalRecommendationUri(uriBuilder, request))
                    .retrieve()
                    .bodyToMono(AiDongneRecommendationResponse.class)
                    .block();
        } catch (WebClientResponseException exception) {
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_API_CALL_FAILED, exception);
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_API_CALL_FAILED, exception);
        }
    }

    private java.net.URI buildPersonalRecommendationUri(
            UriBuilder uriBuilder,
            PersonalRecommendationRequest request
    ) {
        UriBuilder builder = uriBuilder.path(PERSONAL_RECOMMENDATIONS_PATH)
                .queryParam("q1", request.getQ1())
                .queryParam("q2", request.getQ2())
                .queryParam("q3", request.getQ3())
                .queryParam("q4", request.getQ4())
                .queryParam("q5", request.getQ5())
                .queryParam("q6", request.getQ6())
                .queryParam("q7", request.getQ7())
                .queryParam("q8", request.getQ8())
                .queryParam("q9", request.getQ9())
                .queryParam("q10", request.getQ10());

        if (request.getUserId() != null && request.getUserId() >= 1) {
            builder.queryParam("user_id", request.getUserId());
        }

        return builder.build();
    }
}
