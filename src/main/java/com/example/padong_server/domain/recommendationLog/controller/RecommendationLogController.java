package com.example.padong_server.domain.recommendationLog.controller;

import com.example.padong_server.domain.recommendationLog.dto.RecommendationLogClickRequest;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationLogDwellTimeRequest;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationLogInteractionRequest;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationLogLikeRequest;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationLogRow;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationLogUpdateResponse;
import com.example.padong_server.domain.recommendationLog.service.RecommendationLogService;
import com.example.padong_server.global.ResponseDTO;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "추천 로그", description = "AI 동네 추천 노출·클릭·좋아요·체류시간 사용자 행동 로그 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendation-logs")
public class RecommendationLogController {

    private final RecommendationLogService recommendationLogService;

    @Operation(
            summary = "오늘 최신 추천 로그 단건 조회",
            description = "userId + adminDongCode 기준 오늘 발생한 가장 최근 인상 로그 1건. 없으면 404.")
    @GetMapping("/latest")
    public ResponseEntity<ResponseDTO<RecommendationLogRow>> getLatestTodayLog(
            @RequestParam @Min(1) Long userId,
            @RequestParam @NotBlank String adminDongCode
    ) {
        RecommendationLogRow response = recommendationLogService.findLatestToday(userId, adminDongCode)
                .orElseThrow(() -> new CustomException(ErrorCode.RECOMMENDATION_LOG_NOT_FOUND));
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "추천 로그 조회 성공", response));
    }

    @Operation(
            summary = "추천 카드 클릭 카운트 증가",
            description = "사용자가 추천 결과 카드를 클릭할 때마다 호출. clickCount += 1.")
    @PatchMapping("/click")
    public ResponseEntity<ResponseDTO<RecommendationLogUpdateResponse>> updateClick(
            @Valid @RequestBody RecommendationLogClickRequest request
    ) {
        RecommendationLogUpdateResponse response =
                recommendationLogService.incrementClick(request.userId(), request.adminDongCode());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "추천 클릭 로그 업데이트 성공", response));
    }

    @Operation(
            summary = "추천 카드 좋아요 토글 로그",
            description = "추천 결과 카드의 하트 토글 상태 (true/false) 를 로그에 저장.")
    @PatchMapping("/like")
    public ResponseEntity<ResponseDTO<RecommendationLogUpdateResponse>> updateLike(
            @Valid @RequestBody RecommendationLogLikeRequest request
    ) {
        RecommendationLogUpdateResponse response =
                recommendationLogService.updateLiked(request.userId(), request.adminDongCode(), request.liked());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "추천 좋아요 로그 업데이트 성공", response));
    }

    @Operation(
            summary = "추천 카드 체류시간 누적",
            description = "추천 결과 카드를 본 시간 (초). dwellTimeSec 합산.")
    @PatchMapping("/dwell-time")
    public ResponseEntity<ResponseDTO<RecommendationLogUpdateResponse>> updateDwellTime(
            @Valid @RequestBody RecommendationLogDwellTimeRequest request
    ) {
        RecommendationLogUpdateResponse response = recommendationLogService.updateDwellTime(
                request.userId(), request.adminDongCode(), request.dwellTimeSec());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "추천 체류시간 로그 업데이트 성공", response));
    }

    @Operation(
            summary = "추천 카드 통합 상호작용 로그",
            description = "클릭·좋아요·체류시간을 한 번에 갱신. 카드 이탈/언마운트 시점 배치 전송용.")
    @PatchMapping("/interaction")
    public ResponseEntity<ResponseDTO<RecommendationLogUpdateResponse>> updateInteraction(
            @Valid @RequestBody RecommendationLogInteractionRequest request
    ) {
        RecommendationLogUpdateResponse response = recommendationLogService.updateInteraction(
                request.userId(),
                request.adminDongCode(),
                request.clicked(),
                request.liked(),
                request.dwellTimeSec());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "추천 상호작용 로그 업데이트 성공", response));
    }
}
