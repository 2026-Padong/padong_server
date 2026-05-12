package com.example.padong_server.domain.dongneLike.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongneLike.dto.DongneLikeToggleResponse;
import com.example.padong_server.domain.dongneLike.dto.LikedDongneResponse;
import com.example.padong_server.domain.dongneLike.entity.DongneLike;
import com.example.padong_server.domain.dongneLike.repository.DongneLikeRepository;
import com.example.padong_server.domain.population.entity.PopulationDensity;
import com.example.padong_server.domain.population.repository.PopulationDensityRepository;
import com.example.padong_server.domain.rentPrice.entity.RentPrice;
import com.example.padong_server.domain.rentPrice.repository.RentPriceRepository;
import com.example.padong_server.global.CursorPageResponse;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DongneLikeService {

    private final DongneLikeRepository dongneLikeRepository;
    private final AdminDongRepository adminDongRepository;
    private final PopulationDensityRepository populationDensityRepository;
    private final RentPriceRepository rentPriceRepository;

    @Transactional
    public DongneLikeToggleResponse toggleLike(String adminDongCode, Long userId) {
        validate(adminDongCode, userId);

        AdminDong adminDong = adminDongRepository.getByAdminDongCode(adminDongCode);
        DongneLike existingLike =
                dongneLikeRepository
                        .findByAdminDongIdAndUserId(adminDong.getId(), userId)
                        .orElse(null);
        boolean liked;

        if (existingLike != null) {
            dongneLikeRepository.delete(existingLike);
            liked = false;
        } else {
            dongneLikeRepository.save(
                    DongneLike.builder().adminDong(adminDong).userId(userId).build());
            liked = true;
        }

        return DongneLikeToggleResponse.builder()
                .adminDongId(adminDong.getId())
                .adminDongCode(adminDong.getAdminDongCode())
                .userId(userId)
                .liked(liked)
                .likeCount(dongneLikeRepository.countByAdminDongId(adminDong.getId()))
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long adminDongId, Long userId) {
        if (userId == null) {
            return false;
        }
        validateAdminDongId(adminDongId);
        return dongneLikeRepository.existsByAdminDongIdAndUserId(adminDongId, userId);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<LikedDongneResponse> getMyLikes(
            Long userId, Long cursor, int size, String q) {
        int cappedSize = Math.min(Math.max(size, 1), 50);
        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim();

        List<DongneLike> fetched =
                dongneLikeRepository.findMyLikesCursor(
                        userId, cursor, normalizedQ, PageRequest.of(0, cappedSize + 1));

        List<Long> adminDongIds = fetched.stream().map(dl -> dl.getAdminDong().getId()).toList();
        List<String> adminDongCodes =
                fetched.stream().map(dl -> dl.getAdminDong().getAdminDongCode()).toList();

        Map<Long, PopulationDensity> densityByDongId =
                adminDongIds.isEmpty()
                        ? Map.of()
                        : populationDensityRepository
                                .findAllByAdminDongIdIn(adminDongIds)
                                .stream()
                                .collect(
                                        Collectors.toMap(
                                                p -> p.getAdminDong().getId(), p -> p));
        Map<String, List<RentPrice>> rentByDongCode =
                adminDongCodes.isEmpty()
                        ? Map.of()
                        : rentPriceRepository
                                .findAllByAdminDongAdminDongCodeIn(adminDongCodes)
                                .stream()
                                .collect(
                                        Collectors.groupingBy(
                                                rp -> rp.getAdminDong().getAdminDongCode()));

        return CursorPageResponse.from(
                fetched,
                cappedSize,
                DongneLike::getId,
                dl ->
                        LikedDongneResponse.from(
                                dl,
                                densityByDongId.get(dl.getAdminDong().getId()),
                                rentByDongCode.getOrDefault(
                                        dl.getAdminDong().getAdminDongCode(), List.of())));
    }

    @Transactional(readOnly = true)
    public long getLikeCount(Long adminDongId) {
        validateAdminDongId(adminDongId);
        return dongneLikeRepository.countByAdminDongId(adminDongId);
    }

    private void validate(String adminDongCode, Long userId) {
        if (adminDongCode == null || adminDongCode.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_DONGNE_LIKE_REQUEST);
        }
        if (userId == null || userId < 1) {
            throw new CustomException(ErrorCode.INVALID_DONGNE_LIKE_REQUEST);
        }
    }

    private void validateAdminDongId(Long adminDongId) {
        if (adminDongId == null || adminDongId < 1) {
            throw new CustomException(ErrorCode.INVALID_DONGNE_LIKE_REQUEST);
        }
    }
}
