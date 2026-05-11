package com.example.padong_server.domain.storeLike.service;

import com.example.padong_server.domain.storeLike.dto.LikedStoreResponse;
import com.example.padong_server.domain.storeLike.dto.StoreLikeToggleResponse;
import com.example.padong_server.domain.storeLike.entity.StoreLike;
import com.example.padong_server.domain.storeLike.repository.StoreLikeRepository;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.repository.StoreRegistrationRepository;
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
public class StoreLikeService {

    private final StoreLikeRepository storeLikeRepository;
    private final StoreRegistrationRepository storeRegistrationRepository;

    @Transactional
    public StoreLikeToggleResponse toggleLike(Long storeId, Long userId) {
        validate(storeId, userId);

        Store store = findStore(storeId);
        StoreLike existingLike = storeLikeRepository.findByStoreIdAndUserId(storeId, userId)
                .orElse(null);
        boolean liked;

        if (existingLike != null) {
            storeLikeRepository.delete(existingLike);
            liked = false;
        } else {
            storeLikeRepository.save(StoreLike.builder()
                    .store(store)
                    .userId(userId)
                    .build());
            liked = true;
        }

        return StoreLikeToggleResponse.builder()
                .storeId(storeId)
                .userId(userId)
                .liked(liked)
                .likeCount(storeLikeRepository.countByStoreId(storeId))
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long storeId, Long userId) {
        if (userId == null) {
            return false;
        }

        validateStoreId(storeId);
        return storeLikeRepository.existsByStoreIdAndUserId(storeId, userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<LikedStoreResponse> getMyLikes(Long userId, Pageable pageable) {
        Page<StoreLike> page = storeLikeRepository.findByUserIdOrderByIdDesc(userId, pageable);
        List<LikedStoreResponse> content =
                page.getContent().stream().map(LikedStoreResponse::from).toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public long getLikeCount(Long storeId) {
        validateStoreId(storeId);
        return storeLikeRepository.countByStoreId(storeId);
    }

    private Store findStore(Long storeId) {
        return storeRegistrationRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
    }

    private void validate(Long storeId, Long userId) {
        validateStoreId(storeId);
        if (userId == null || userId < 1) {
            throw new CustomException(ErrorCode.INVALID_STORE_LIKE_REQUEST);
        }
    }

    private void validateStoreId(Long storeId) {
        if (storeId == null || storeId < 1) {
            throw new CustomException(ErrorCode.INVALID_STORE_LIKE_REQUEST);
        }
    }
}
