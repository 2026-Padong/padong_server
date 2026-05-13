package com.example.padong_server.domain.recommendation.dto;

import com.example.padong_server.domain.recommendation.entity.UserPreferenceAnswer;
import java.time.LocalDateTime;

public record PreferenceAnswersResponse(
        Integer q1, Integer q2, Integer q3, Integer q4, Integer q5,
        Integer q6, Integer q7, Integer q8, Integer q9, Integer q10,
        LocalDateTime updatedAt
) {
    public static PreferenceAnswersResponse from(UserPreferenceAnswer entity) {
        return new PreferenceAnswersResponse(
                entity.getQ1(), entity.getQ2(), entity.getQ3(), entity.getQ4(), entity.getQ5(),
                entity.getQ6(), entity.getQ7(), entity.getQ8(), entity.getQ9(), entity.getQ10(),
                entity.getUpdatedAt());
    }
}
