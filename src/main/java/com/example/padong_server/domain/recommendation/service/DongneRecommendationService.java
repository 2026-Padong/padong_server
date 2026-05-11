package com.example.padong_server.domain.recommendation.service;

import com.example.padong_server.domain.dongne.dto.DongneSummaryResponse;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.recommendation.client.AiRecommendationClient;
import com.example.padong_server.domain.recommendation.dto.AiDongneRecommendationResponse;
import com.example.padong_server.domain.recommendation.dto.DongneRecommendationResponse;
import com.example.padong_server.domain.recommendation.dto.PersonalRecommendationRequest;
import com.example.padong_server.domain.recommendationLog.dto.RecommendationResultLogItem;
import com.example.padong_server.domain.recommendationLog.service.RecommendationLogService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DongneRecommendationService {

    private final AiRecommendationClient aiRecommendationClient;
    private final AdminDongRepository adminDongRepository;
    private final RecommendationLogService recommendationLogService;

    @Transactional(readOnly = true)
    public DongneRecommendationResponse getPersonalRecommendations(PersonalRecommendationRequest request) {
        AiDongneRecommendationResponse aiResponse =
                aiRecommendationClient.getPersonalRecommendations(request);
        List<Long> recommendationIds = aiResponse == null || aiResponse.recommendations() == null
                ? Collections.emptyList()
                : aiResponse.recommendations();

        Map<Long, AdminDong> adminDongById = adminDongRepository.findAllById(recommendationIds).stream()
                .collect(Collectors.toMap(AdminDong::getId, Function.identity()));
        List<DongneSummaryResponse> recommendations = recommendationIds.stream()
                .map(adminDongById::get)
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .toList();

        if (request.getUserId() != null) {
            saveImpressionLogs(request, recommendationIds, adminDongById);
        }

        return new DongneRecommendationResponse(
                aiResponse == null ? null : aiResponse.userType(),
                recommendationIds,
                recommendations);
    }

    private void saveImpressionLogs(
            PersonalRecommendationRequest request,
            List<Long> recommendationIds,
            Map<Long, AdminDong> adminDongById
    ) {
        List<RecommendationResultLogItem> logItems = recommendationIds.stream()
                .map(adminDongById::get)
                .filter(Objects::nonNull)
                .map(adminDong -> new RecommendationResultLogItem(adminDong.getAdminDongCode()))
                .toList();
        recommendationLogService.savePersonalImpressionLogs(
                request.getUserId(),
                logItems,
                request.toSurveyAnswers());
    }

    private DongneSummaryResponse toResponse(AdminDong adminDong) {
        return DongneSummaryResponse.builder()
                .adminDongCode(adminDong.getAdminDongCode())
                .cityName(adminDong.getCityName())
                .districtName(adminDong.getDistrictName())
                .adminDongName(adminDong.getAdminDongName())
                .address("%s %s %s".formatted(
                        adminDong.getCityName(),
                        adminDong.getDistrictName(),
                        adminDong.getAdminDongName()))
                .latitude(adminDong.getLatitude())
                .longitude(adminDong.getLongitude())
                .build();
    }
}
