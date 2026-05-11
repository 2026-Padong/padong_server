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

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendation-logs")
public class RecommendationLogController {

    private final RecommendationLogService recommendationLogService;

    @GetMapping("/latest")
    public ResponseEntity<ResponseDTO<RecommendationLogRow>> getLatestTodayLog(
            @RequestParam @Min(1) Long userId,
            @RequestParam @NotBlank String adminDongCode
    ) {
        RecommendationLogRow response = recommendationLogService.findLatestToday(userId, adminDongCode)
                .orElseThrow(() -> new CustomException(ErrorCode.RECOMMENDATION_LOG_NOT_FOUND));
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "추천 로그 조회 성공", response));
    }

    @PatchMapping("/click")
    public ResponseEntity<ResponseDTO<RecommendationLogUpdateResponse>> updateClick(
            @Valid @RequestBody RecommendationLogClickRequest request
    ) {
        RecommendationLogUpdateResponse response =
                recommendationLogService.incrementClick(request.userId(), request.adminDongCode());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "추천 클릭 로그 업데이트 성공", response));
    }

    @PatchMapping("/like")
    public ResponseEntity<ResponseDTO<RecommendationLogUpdateResponse>> updateLike(
            @Valid @RequestBody RecommendationLogLikeRequest request
    ) {
        RecommendationLogUpdateResponse response =
                recommendationLogService.updateLiked(request.userId(), request.adminDongCode(), request.liked());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "추천 좋아요 로그 업데이트 성공", response));
    }

    @PatchMapping("/dwell-time")
    public ResponseEntity<ResponseDTO<RecommendationLogUpdateResponse>> updateDwellTime(
            @Valid @RequestBody RecommendationLogDwellTimeRequest request
    ) {
        RecommendationLogUpdateResponse response = recommendationLogService.updateDwellTime(
                request.userId(), request.adminDongCode(), request.dwellTimeSec());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "추천 체류시간 로그 업데이트 성공", response));
    }

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
