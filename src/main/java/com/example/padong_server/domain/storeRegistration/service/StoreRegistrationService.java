package com.example.padong_server.domain.storeRegistration.service;

import com.example.padong_server.domain.storeLike.service.StoreLikeService;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationCreateRequest;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationResponse;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationUpdateRequest;
import com.example.padong_server.domain.storeRegistration.entity.StoreRegistration;
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
    public StoreRegistrationResponse createStore(StoreRegistrationCreateRequest request) {
        validate(request);

        StoreRegistration storeRegistration = StoreRegistration.builder()
                .name(request.name().trim())
                .roadAddress(request.address().trim())
                .phoneNumber(request.phoneNumber().trim())
                .operatingHours(request.operatingHours().trim())
                .build();

        return toResponse(storeRegistrationRepository.save(storeRegistration), null);
    }

    @Transactional(readOnly = true)
    public StoreRegistrationResponse getStore(Long storeId, Long userId) {
        return toResponse(findStore(storeId), userId);
    }

    @Transactional
    public StoreRegistrationResponse updateStore(Long storeId, StoreRegistrationUpdateRequest request) {
        validate(request);

        StoreRegistration storeRegistration = findStore(storeId);
        storeRegistration.updateBasicInfo(
                request.name().trim(),
                request.address().trim(),
                request.phoneNumber().trim(),
                request.operatingHours().trim()
        );

        return toResponse(storeRegistration, null);
    }

    @Transactional
    public void deleteStore(Long storeId) {
        StoreRegistration storeRegistration = findStore(storeId);
        storeRegistrationRepository.delete(storeRegistration);
    }

    private StoreRegistration findStore(Long storeId) {
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

    private StoreRegistrationResponse toResponse(StoreRegistration storeRegistration, Long userId) {
        return StoreRegistrationResponse.builder()
                .id(storeRegistration.getId())
                .name(storeRegistration.getName())
                .address(storeRegistration.getRoadAddress())
                .phoneNumber(storeRegistration.getPhoneNumber())
                .operatingHours(storeRegistration.getOperatingHours())
                .likeCount(storeLikeService.getLikeCount(storeRegistration.getId()))
                .likedByCurrentUser(storeLikeService.isLikedByUser(storeRegistration.getId(), userId))
                .build();
    }
}
