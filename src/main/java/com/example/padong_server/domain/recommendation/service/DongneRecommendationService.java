package com.example.padong_server.domain.recommendation.service;

import com.example.padong_server.domain.activityMobility.dto.MobilitySimpleResponse;
import com.example.padong_server.domain.activityMobility.service.SafetyIndexService;
import com.example.padong_server.domain.dongne.boundary.AdminDongBoundaryService;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongneLike.service.DongneLikeService;
import com.example.padong_server.domain.recommendation.client.AiRecommendationClient;
import com.example.padong_server.domain.recommendation.dto.AiDongneRecommendationResponse;
import com.example.padong_server.domain.recommendation.dto.DongneRecommendationResponse;
import com.example.padong_server.domain.recommendation.dto.PersonalRecommendationRequest;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationResultLogItem;
import com.example.padong_server.domain.recommendationLog.service.RecommendationLogService;
import com.example.padong_server.global.PageResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class DongneRecommendationService {

    private final AiRecommendationClient aiRecommendationClient;
    private final AdminDongRepository adminDongRepository;
    private final RecommendationLogService recommendationLogService;
    private final AdminDongBoundaryService boundaryService;
    private final DongneLikeService dongneLikeService;
    private final SafetyIndexService safetyIndexService;
    private final UserPreferenceAnswerService userPreferenceAnswerService;

    @Transactional
    public DongneRecommendationResponse getPersonalRecommendations(
            PersonalRecommendationRequest request, int page, int size) {
        // 인증된 유저면 답변 upsert — 결과 페이지 새로고침/공유 시 재호출용
        userPreferenceAnswerService.upsert(request.getUserId(), request);

        AiDongneRecommendationResponse aiResponse =
                aiRecommendationClient.getPersonalRecommendations(request);
        List<Long> recommendationIds =
                aiResponse == null || aiResponse.recommendations() == null
                        ? Collections.emptyList()
                        : aiResponse.recommendations();

        Map<Long, AdminDong> adminDongById =
                adminDongRepository.findAllById(recommendationIds).stream()
                        .collect(Collectors.toMap(AdminDong::getId, Function.identity()));

        // 입력 순서 유지하면서 매핑 안 된 ID 는 drop
        List<AdminDong> orderedDongs =
                recommendationIds.stream()
                        .map(adminDongById::get)
                        .filter(Objects::nonNull)
                        .toList();

        // 좋아요 인상 로그는 전체 결과 기준 (페이지와 무관)
        if (request.getUserId() != null) {
            saveImpressionLogs(request, orderedDongs);
        }

        // 페이지 슬라이스
        int total = orderedDongs.size();
        int cappedSize = Math.min(Math.max(size, 1), 50);
        int pageNum = Math.max(page, 0);
        int from = pageNum * cappedSize;
        int to = Math.min(from + cappedSize, total);
        List<AdminDong> pageDongs = from >= total ? List.of() : orderedDongs.subList(from, to);

        // 페이지 행정동만 batch fetch (boundary)
        Map<String, JsonNode> boundaryByCode =
                boundaryService.findFeaturesByCodes(
                        pageDongs.stream().map(AdminDong::getAdminDongCode).toList());

        Long currentUserId = request.getUserId();
        List<MobilitySimpleResponse> content =
                pageDongs.stream()
                        .map(
                                dong ->
                                        MobilitySimpleResponse.forRecommendation(
                                                dong,
                                                safetyIndexService.getOverallGrade(
                                                        dong.getCityName(),
                                                        dong.getDistrictName()),
                                                null,
                                                boundaryByCode.get(dong.getAdminDongCode()),
                                                dongneLikeService.getLikeCount(dong.getId()),
                                                dongneLikeService.isLikedByUser(
                                                        dong.getId(), currentUserId)))
                        .toList();

        PageResponse<MobilitySimpleResponse> pageResponse =
                PageResponse.of(content, pageNum, cappedSize, total);

        return new DongneRecommendationResponse(
                aiResponse == null ? null : aiResponse.userType(), pageResponse);
    }

    private void saveImpressionLogs(
            PersonalRecommendationRequest request, List<AdminDong> orderedDongs) {
        List<RecommendationResultLogItem> logItems =
                orderedDongs.stream()
                        .map(adminDong -> new RecommendationResultLogItem(adminDong.getAdminDongCode()))
                        .toList();
        recommendationLogService.savePersonalImpressionLogs(
                request.getUserId(), logItems, request.toSurveyAnswers());
    }
}
