package com.example.padong_server.domain.dongneLike.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongneLike.dto.DongneLikeToggleResponse;
import com.example.padong_server.domain.dongneLike.dto.LikedDongneResponse;
import com.example.padong_server.domain.dongneLike.entity.DongneLike;
import com.example.padong_server.domain.dongneLike.repository.DongneLikeRepository;
import com.example.padong_server.global.PageResponse;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DongneLikeService {

    private final DongneLikeRepository dongneLikeRepository;
    private final AdminDongRepository adminDongRepository;

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
    public PageResponse<LikedDongneResponse> getMyLikes(Long userId, Pageable pageable) {
        Page<DongneLike> page =
                dongneLikeRepository.findByUserIdOrderByIdDesc(userId, pageable);
        List<LikedDongneResponse> content =
                page.getContent().stream().map(LikedDongneResponse::from).toList();
        return PageResponse.from(page, content);
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
