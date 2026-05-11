package com.example.padong_server.domain.recommendation.controller;

import com.example.padong_server.domain.recommendation.dto.DongneRecommendationResponse;
import com.example.padong_server.domain.recommendation.dto.PersonalRecommendationRequest;
import com.example.padong_server.domain.recommendation.service.DongneRecommendationService;
import com.example.padong_server.global.ResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping({"/dongne/recommendations", "/api/v1/dongne/recommendations"})
public class DongneRecommendationController {

    private final DongneRecommendationService dongneRecommendationService;

    @GetMapping
    public ResponseEntity<ResponseDTO<DongneRecommendationResponse>> getPersonalRecommendations(
            @Valid @ModelAttribute PersonalRecommendationRequest request
    ) {
        DongneRecommendationResponse response =
                dongneRecommendationService.getPersonalRecommendations(request);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "동네 추천 조회 성공", response));
    }
}
