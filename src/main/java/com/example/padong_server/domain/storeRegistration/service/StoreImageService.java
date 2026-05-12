package com.example.padong_server.domain.storeRegistration.service;

import com.example.padong_server.domain.storeRegistration.dto.StoreImageResponse;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.entity.StoreImage;
import com.example.padong_server.domain.storeRegistration.repository.StoreImageRepository;
import com.example.padong_server.domain.upload.service.S3FileUploader;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StoreImageService {

    private final StoreRegistrationService storeRegistrationService;
    private final StoreImageRepository storeImageRepository;
    private final S3FileUploader s3FileUploader;

    @Transactional(readOnly = true)
    public List<StoreImageResponse> list(Long storeId) {
        // permitAll endpoint 이라 owner 검증 없이 가게 존재만 확인.
        storeRegistrationService.getStore(storeId, null);
        return storeImageRepository.findByStoreIdOrderBySortOrderAsc(storeId).stream()
                .map(StoreImageResponse::from)
                .toList();
    }

    @Transactional
    public String replaceThumbnail(Long storeId, Long currentUserId, MultipartFile file) {
        Store store = storeRegistrationService.findOwnedStore(storeId, currentUserId);
        String previous = store.getThumbnailUrl();
        String url = s3FileUploader.uploadStoreThumbnail(file, storeId);
        store.changeThumbnailUrl(url);
        s3FileUploader.deleteIfOwned(previous);
        return url;
    }

    @Transactional
    public void removeThumbnail(Long storeId, Long currentUserId) {
        Store store = storeRegistrationService.findOwnedStore(storeId, currentUserId);
        String previous = store.getThumbnailUrl();
        store.changeThumbnailUrl(null);
        s3FileUploader.deleteIfOwned(previous);
    }

    @Transactional
    public StoreImageResponse addImage(Long storeId, Long currentUserId, MultipartFile file) {
        Store store = storeRegistrationService.findOwnedStore(storeId, currentUserId);
        String url = s3FileUploader.uploadStoreImage(file, storeId);
        int nextOrder = storeImageRepository.countByStoreId(storeId);
        StoreImage image = StoreImage.builder().store(store).url(url).sortOrder(nextOrder).build();
        return StoreImageResponse.from(storeImageRepository.save(image));
    }

    @Transactional
    public void removeImage(Long storeId, Long imageId, Long currentUserId) {
        storeRegistrationService.findOwnedStore(storeId, currentUserId);
        StoreImage image =
                storeImageRepository
                        .findById(imageId)
                        .orElseThrow(() -> new CustomException(ErrorCode.STORE_IMAGE_NOT_FOUND));
        if (!image.getStore().getId().equals(storeId)) {
            throw new CustomException(ErrorCode.STORE_IMAGE_NOT_FOUND);
        }
        storeImageRepository.delete(image);
        s3FileUploader.deleteIfOwned(image.getUrl());
        // 삭제 후 sort_order 압축 — 0..n-1 로 재정렬
        List<StoreImage> remaining =
                storeImageRepository.findByStoreIdOrderBySortOrderAsc(storeId);
        for (int i = 0; i < remaining.size(); i++) {
            if (remaining.get(i).getSortOrder() != i) {
                remaining.get(i).changeSortOrder(i);
            }
        }
    }

    @Transactional
    public List<StoreImageResponse> reorder(Long storeId, Long currentUserId, List<Long> ids) {
        storeRegistrationService.findOwnedStore(storeId, currentUserId);
        List<StoreImage> existing = storeImageRepository.findByStoreIdOrderBySortOrderAsc(storeId);

        if (existing.size() != ids.size()) {
            throw new CustomException(
                    ErrorCode.STORE_IMAGE_NOT_FOUND, "현재 갤러리 이미지 수와 요청 id 수가 다릅니다.");
        }
        Set<Long> existingIds = new HashSet<>();
        Map<Long, StoreImage> byId = new HashMap<>();
        for (StoreImage img : existing) {
            existingIds.add(img.getId());
            byId.put(img.getId(), img);
        }
        if (!existingIds.equals(new HashSet<>(ids))) {
            throw new CustomException(
                    ErrorCode.STORE_IMAGE_NOT_FOUND, "요청 id 와 현재 갤러리 구성이 일치하지 않습니다.");
        }

        for (int i = 0; i < ids.size(); i++) {
            StoreImage img = byId.get(ids.get(i));
            if (img.getSortOrder() != i) {
                img.changeSortOrder(i);
            }
        }
        return storeImageRepository.findByStoreIdOrderBySortOrderAsc(storeId).stream()
                .map(StoreImageResponse::from)
                .toList();
    }
}
