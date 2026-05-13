package com.example.padong_server.domain.recommendation.controller;

import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.recommendation.dto.PreferenceAnswersResponse;
import com.example.padong_server.domain.recommendation.service.UserPreferenceAnswerService;
import com.example.padong_server.global.ResponseDTO;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preference")
@RequiredArgsConstructor
@Tag(name = "취향 설문", description = "사용자 취향 설문 답변 조회")
public class PreferenceAnswerController {

    private final UserPreferenceAnswerService userPreferenceAnswerService;

    @GetMapping("/me/answers")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
            summary = "내 취향 설문 답변 조회",
            description = "마지막으로 제출한 q1~q10 답변 + updatedAt. 답변 없으면 404. JWT 필수.")
    public ResponseEntity<ResponseDTO<PreferenceAnswersResponse>> getMyAnswers(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_USER);
        }
        PreferenceAnswersResponse response = userPreferenceAnswerService
                .findByUserId(userDetails.getUserId())
                .map(PreferenceAnswersResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.PREFERENCE_ANSWERS_NOT_FOUND));
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "취향 답변 조회 성공", response));
    }
}
