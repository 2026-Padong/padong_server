package com.example.padong_server.domain.storeRegistration.service;

import com.example.padong_server.domain.storeLike.service.StoreLikeService;
import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationCreateRequest;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationResponse;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationUpdateRequest;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.repository.StoreRegistrationRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StoreRegistrationService {

    private final StoreRegistrationRepository storeRegistrationRepository;
    private final StoreLikeService storeLikeService;

    @Transactional
    public StoreRegistrationResponse createStore(User owner, StoreRegistrationCreateRequest request) {
        validate(request);

        Store store = Store.builder()
                .name(request.name().trim())
                .roadAddress(request.address().trim())
                .phoneNumber(request.phoneNumber().trim())
                .operatingHours(request.operatingHours().trim())
                .owner(owner)
                .build();

        return toResponse(storeRegistrationRepository.save(store), null);
    }

    @Transactional(readOnly = true)
    public StoreRegistrationResponse getStore(Long storeId, Long userId) {
        return toResponse(findStore(storeId), userId);
    }

    @Transactional
    public StoreRegistrationResponse updateStore(Long storeId, StoreRegistrationUpdateRequest request) {
        validate(request);

        Store store = findStore(storeId);
        store.updateBasicInfo(
                request.name().trim(),
                request.address().trim(),
                request.phoneNumber().trim(),
                request.operatingHours().trim()
        );

        return toResponse(store, null);
    }

    @Transactional
    public void deleteStore(Long storeId) {
        Store store = findStore(storeId);
        storeRegistrationRepository.delete(store);
    }

    private Store findStore(Long storeId) {
        return storeRegistrationRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
    }

    private void validate(StoreRegistrationCreateRequest request) {
        validateCreateCommon(
                request.name(),
                request.address(),
                request.phoneNumber(),
                request.operatingHours()
        );
    }

    private void validate(StoreRegistrationUpdateRequest request) {
        validateBasicInfo(
                request.name(),
                request.address(),
                request.phoneNumber(),
                request.operatingHours()
        );
    }

    private void validateCreateCommon(
            String name,
            String address,
            String phoneNumber,
            String operatingHours
    ) {
        if (!StringUtils.hasText(name)
                || !StringUtils.hasText(address)
                || !StringUtils.hasText(phoneNumber)
                || !StringUtils.hasText(operatingHours)) {
            throw new CustomException(ErrorCode.INVALID_STORE_REQUEST);
        }
    }

    private void validateBasicInfo(
            String name,
            String address,
            String phoneNumber,
            String operatingHours
    ) {
        if (!StringUtils.hasText(name)
                || !StringUtils.hasText(address)
                || !StringUtils.hasText(phoneNumber)
                || !StringUtils.hasText(operatingHours)) {
            throw new CustomException(ErrorCode.INVALID_STORE_REQUEST);
        }
    }

    private StoreRegistrationResponse toResponse(Store store, Long userId) {
        return StoreRegistrationResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .address(store.getRoadAddress())
                .phoneNumber(store.getPhoneNumber())
                .operatingHours(store.getOperatingHours())
                .likeCount(storeLikeService.getLikeCount(store.getId()))
                .likedByCurrentUser(storeLikeService.isLikedByUser(store.getId(), userId))
                .build();
    }
}
