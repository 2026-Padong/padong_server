package com.example.padong_server.domain.recommendationLog.service;

import com.example.padong_server.domain.recommendationLog.dto.RecommendationLogRow;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationLogUpdateResponse;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationResultLogItem;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationSurveyAnswers;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationType;
import com.example.padong_server.domain.recommendationLog.repository.RecommendationLogJdbcRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationLogService {

    private final RecommendationLogJdbcRepository recommendationLogJdbcRepository;

    public void savePersonalImpressionLogs(
            Long userId,
            List<RecommendationResultLogItem> recommendations,
            RecommendationSurveyAnswers answers
    ) {
        saveImpressionLogs(userId, recommendations, answers, RecommendationType.PERSONAL);
    }

    public void saveImpressionLogs(
            Long userId,
            List<RecommendationResultLogItem> recommendations,
            RecommendationSurveyAnswers answers,
            RecommendationType recommendationType
    ) {
        if (userId == null || recommendations == null || recommendations.isEmpty()) {
            return;
        }
        RecommendationSurveyAnswers normalizedAnswers = answers == null
                ? new RecommendationSurveyAnswers(null, null, null, null, null, null, null, null, null, null)
                : answers;
        RecommendationType normalizedType = recommendationType == null
                ? RecommendationType.PERSONAL
                : recommendationType;

        try {
            int[] result = recommendationLogJdbcRepository.batchInsertImpressions(
                    userId, recommendations, normalizedAnswers, normalizedType);
            int successCount = (int) Arrays.stream(result).filter(count -> count > 0).count();
            log.info("Recommendation impression logs inserted. userId={}, count={}", userId, successCount);
        } catch (RuntimeException exception) {
            log.warn("Failed to insert recommendation impression logs. userId={}", userId, exception);
        }
    }

    public Optional<RecommendationLogRow> findLatestToday(Long userId, String adminDongCode) {
        validateUserAndAdminDong(userId, adminDongCode);
        Optional<RecommendationLogRow> logRow =
                recommendationLogJdbcRepository.findLatestToday(userId, adminDongCode);
        if (logRow.isEmpty()) {
            log.warn("Recommendation log not found. userId={}, adminDongCode={}", userId, adminDongCode);
        }
        return logRow;
    }

    public RecommendationLogUpdateResponse incrementClick(Long userId, String adminDongCode) {
        validateUserAndAdminDong(userId, adminDongCode);
        int updated = recommendationLogJdbcRepository.incrementClickedCount(userId, adminDongCode);
        log.info(
                "Recommendation click log update. userId={}, adminDongCode={}, updated={}",
                userId,
                adminDongCode,
                updated > 0);
        ensureUpdated(updated, userId, adminDongCode);
        return new RecommendationLogUpdateResponse(userId, adminDongCode, true);
    }

    public RecommendationLogUpdateResponse updateLiked(Long userId, String adminDongCode, boolean liked) {
        validateUserAndAdminDong(userId, adminDongCode);
        int updated = recommendationLogJdbcRepository.updateLiked(userId, adminDongCode, liked);
        log.info(
                "Recommendation like log update. userId={}, adminDongCode={}, liked={}, updated={}",
                userId,
                adminDongCode,
                liked,
                updated > 0);
        ensureUpdated(updated, userId, adminDongCode);
        return new RecommendationLogUpdateResponse(userId, adminDongCode, true);
    }

    public RecommendationLogUpdateResponse updateDwellTime(Long userId, String adminDongCode, int dwellTimeSec) {
        validateUserAndAdminDong(userId, adminDongCode);
        if (dwellTimeSec < 0) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "dwellTimeSec는 0 이상이어야 합니다.");
        }

        int updated = recommendationLogJdbcRepository.updateDwellTimeSec(userId, adminDongCode, dwellTimeSec);
        log.info(
                "Recommendation dwell time log update. userId={}, adminDongCode={}, dwellTimeSec={}, updated={}",
                userId,
                adminDongCode,
                dwellTimeSec,
                updated > 0);
        ensureUpdated(updated, userId, adminDongCode);
        return new RecommendationLogUpdateResponse(userId, adminDongCode, true);
    }

    public RecommendationLogUpdateResponse updateInteraction(
            Long userId,
            String adminDongCode,
            Boolean clicked,
            Boolean liked,
            Integer dwellTimeSec
    ) {
        validateUserAndAdminDong(userId, adminDongCode);
        if (clicked == null && liked == null && dwellTimeSec == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "업데이트할 상호작용 값이 없습니다.");
        }

        boolean updated = false;
        if (Boolean.TRUE.equals(clicked)) {
            updated |= recommendationLogJdbcRepository.incrementClickedCount(userId, adminDongCode) > 0;
        }
        if (liked != null) {
            updated |= recommendationLogJdbcRepository.updateLiked(userId, adminDongCode, liked) > 0;
        }
        if (dwellTimeSec != null) {
            if (dwellTimeSec < 0) {
                throw new CustomException(ErrorCode.VALIDATION_ERROR, "dwellTimeSec는 0 이상이어야 합니다.");
            }
            updated |= recommendationLogJdbcRepository.updateDwellTimeSec(userId, adminDongCode, dwellTimeSec) > 0;
        }

        log.info(
                "Recommendation interaction log update. userId={}, adminDongCode={}, updated={}",
                userId,
                adminDongCode,
                updated);
        ensureUpdated(updated ? 1 : 0, userId, adminDongCode);
        return new RecommendationLogUpdateResponse(userId, adminDongCode, true);
    }

    private void validateUserAndAdminDong(Long userId, String adminDongCode) {
        if (userId == null || userId < 1 || !StringUtils.hasText(adminDongCode)) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "userId와 adminDongCode를 확인해주세요.");
        }
    }

    private void ensureUpdated(int updated, Long userId, String adminDongCode) {
        if (updated < 1) {
            log.warn("Recommendation log update target not found. userId={}, adminDongCode={}", userId, adminDongCode);
            throw new CustomException(ErrorCode.RECOMMENDATION_LOG_NOT_FOUND);
        }
    }
}
