package com.example.padong_server.domain.storeLike.service;

import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowStatus;
import com.example.padong_server.domain.orderFlow.repository.OrderFlowRepository;
import com.example.padong_server.domain.payment.entity.GroupOrder;
import com.example.padong_server.domain.payment.entity.GroupOrderStatus;
import com.example.padong_server.domain.payment.repository.GroupOrderRepository;
import com.example.padong_server.domain.storeLike.dto.LikedStoreResponse;
import com.example.padong_server.domain.storeLike.dto.StoreLikeToggleResponse;
import com.example.padong_server.domain.storeLike.entity.StoreLike;
import com.example.padong_server.domain.storeLike.repository.StoreLikeRepository;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.repository.StoreRegistrationRepository;
import com.example.padong_server.domain.storeRegistration.service.RecruitmentStatusCalculator;
import com.example.padong_server.global.CursorPageResponse;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreLikeService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final List<OrderFlowStatus> ORDER_FLOW_ACTIVE_STATUSES =
            List.copyOf(EnumSet.of(
                    OrderFlowStatus.PENDING,
                    OrderFlowStatus.WAITING_APPROVAL,
                    OrderFlowStatus.APPROVED,
                    OrderFlowStatus.READY));

    private final StoreLikeRepository storeLikeRepository;
    private final StoreRegistrationRepository storeRegistrationRepository;
    private final GroupOrderRepository groupOrderRepository;
    private final OrderFlowRepository orderFlowRepository;
    private final RecruitmentStatusCalculator recruitmentStatusCalculator;

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
    public CursorPageResponse<LikedStoreResponse> getMyLikes(
            Long userId, Long cursor, int size, String q) {
        int cappedSize = Math.min(Math.max(size, 1), 50);
        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim();

        List<StoreLike> fetched =
                storeLikeRepository.findMyLikesCursor(
                        userId, cursor, normalizedQ, PageRequest.of(0, cappedSize + 1));

        List<Long> storeIds = fetched.stream().map(sl -> sl.getStore().getId()).toList();
        Map<Long, GroupOrder> activeByStoreId =
                storeIds.isEmpty()
                        ? Map.of()
                        : groupOrderRepository
                                .findByStoreIdInAndStatus(storeIds, GroupOrderStatus.OPEN)
                                .stream()
                                .collect(
                                        Collectors.toMap(
                                                go -> go.getStore().getId(),
                                                Function.identity(),
                                                (a, b) -> a.getId() > b.getId() ? a : b));

        Map<Long, OrderFlow> activeFlowByStoreId = new HashMap<>();
        for (Long sid : storeIds) {
            orderFlowRepository
                    .findTopByStoreIdAndStatusInOrderByIdDesc(sid, ORDER_FLOW_ACTIVE_STATUSES)
                    .ifPresent(f -> activeFlowByStoreId.put(sid, f));
        }
        LocalDateTime now = LocalDateTime.now(KST);

        return CursorPageResponse.from(
                fetched,
                cappedSize,
                StoreLike::getId,
                sl ->
                        LikedStoreResponse.from(
                                sl,
                                activeByStoreId.get(sl.getStore().getId()),
                                recruitmentStatusCalculator.calculate(
                                        sl.getStore(),
                                        activeFlowByStoreId.get(sl.getStore().getId()),
                                        now)));
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
