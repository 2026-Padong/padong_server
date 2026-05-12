package com.example.padong_server.domain.recommendation.controller;

import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.recommendation.dto.DongneRecommendationResponse;
import com.example.padong_server.domain.recommendation.dto.PersonalRecommendationRequest;
import com.example.padong_server.domain.recommendation.service.DongneRecommendationService;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping({"/dongne/recommendations", "/api/v1/dongne/recommendations"})
public class DongneRecommendationController {

    private final DongneRecommendationService dongneRecommendationService;

    @GetMapping
    @Operation(
            summary = "취향 기반 동네 추천",
            description =
                    "라이프스타일 설문(q1~q10) 으로 AI 추천. 응답 카드는 출퇴근(/mobility) 과 동일한 "
                            + "MobilitySimpleResponse shape — 좋아요/경계/안전등급 인라인. "
                            + "totalMobility/avgTime 은 추천 카드에선 0.")
    public ResponseEntity<ResponseDTO<DongneRecommendationResponse>> getPersonalRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute PersonalRecommendationRequest request,
            @Parameter(description = "페이지 번호 (0부터)", example = "0")
                    @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기 (기본 10, 최대 50)", example = "10")
                    @RequestParam(required = false, defaultValue = "10") int size) {
        // userId 가 request body 로 들어왔으면 그대로, 안 들어왔으면 JWT 로 보정 — impression 로그에 사용
        if (request.getUserId() == null && userDetails != null) {
            request.setUserId(userDetails.getUserId());
        }
        DongneRecommendationResponse response =
                dongneRecommendationService.getPersonalRecommendations(request, page, size);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "동네 추천 조회 성공", response));
    }
}
