package com.example.padong_server.domain.recommendation.dto;

import com.example.padong_server.domain.recommendationLog.dto.RecommendationSurveyAnswers;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonalRecommendationRequest {

    private Long userId;

    @NotNull
    private Integer q1;

    @NotNull
    private Integer q2;

    @NotNull
    private Integer q3;

    @NotNull
    private Integer q4;

    @NotNull
    private Integer q5;

    @NotNull
    private Integer q6;

    @NotNull
    private Integer q7;

    @NotNull
    private Integer q8;

    @NotNull
    private Integer q9;

    @NotNull
    private Integer q10;

    public RecommendationSurveyAnswers toSurveyAnswers() {
        return new RecommendationSurveyAnswers(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10);
    }

    public void setUser_id(Long userId) {
        this.userId = userId;
    }
}
