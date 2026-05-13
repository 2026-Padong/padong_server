package com.example.padong_server.domain.recommendation.dto;

import com.example.padong_server.domain.recommendationLog.dto.RecommendationSurveyAnswers;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonalRecommendationRequest {

    private Long userId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer q1;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer q2;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer q3;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer q4;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer q5;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer q6;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer q7;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer q8;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer q9;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer q10;

    public RecommendationSurveyAnswers toSurveyAnswers() {
        return new RecommendationSurveyAnswers(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10);
    }
}
