package com.example.padong_server.domain.storeRegistration.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.menu.entity.Menu;
import com.example.padong_server.domain.menu.repository.MenuRepository;
import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.orderFlow.entity.OrderFlowStatus;
import com.example.padong_server.domain.orderFlow.repository.OrderFlowRepository;
import com.example.padong_server.domain.payment.entity.GroupOrder;
import com.example.padong_server.domain.payment.entity.GroupOrderStatus;
import com.example.padong_server.domain.payment.repository.GroupOrderRepository;
import com.example.padong_server.domain.storeLike.service.StoreLikeService;
import com.example.padong_server.domain.storeRegistration.dto.ShopDetailResponse;
import com.example.padong_server.domain.storeRegistration.dto.ShopSummaryResponse;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationCreateRequest;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationResponse;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationUpdateRequest;
import com.example.padong_server.domain.storeRegistration.dto.StoreSearchCriteria;
import com.example.padong_server.domain.storeRegistration.entity.RecruitmentStatus;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.entity.StoreImage;
import com.example.padong_server.domain.storeRegistration.repository.StoreImageRepository;
import com.example.padong_server.domain.storeRegistration.repository.StoreRegistrationRepository;
import com.example.padong_server.domain.storeRegistration.repository.StoreSpecifications;
import java.util.EnumSet;
import com.example.padong_server.global.PageResponse;
import com.example.padong_server.global.client.sk.SkAddress;
import com.example.padong_server.global.client.sk.SkAddressClient;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreRegistrationService {

    private static final List<OrderFlowStatus> ORDER_FLOW_ACTIVE_STATUSES =
            List.copyOf(EnumSet.of(
                    OrderFlowStatus.PENDING,
                    OrderFlowStatus.WAITING_APPROVAL,
                    OrderFlowStatus.APPROVED,
                    OrderFlowStatus.READY));

    private final StoreRegistrationRepository storeRegistrationRepository;
    private final StoreLikeService storeLikeService;
    private final GroupOrderRepository groupOrderRepository;
    private final MenuRepository menuRepository;
    private final StoreImageRepository storeImageRepository;
    private final DongneService dongneService;
    private final AdminDongRepository adminDongRepository;
    private final SkAddressClient skAddressClient;
    private final OrderFlowRepository orderFlowRepository;
    private final RecruitmentStatusCalculator recruitmentStatusCalculator;

    @Transactional
    public StoreRegistrationResponse createStore(User owner, StoreRegistrationCreateRequest request) {
        AdminDong adminDong = resolveAdminDong(request.adminDongCode(), request.address());

        Store store = Store.builder()
                .adminDong(adminDong)
                .name(request.name().trim())
                .category(request.category())
                .address(request.address().trim())
                .phoneNumber(request.phoneNumber().trim())
                .description(request.description())
                .openTime(request.openTime())
                .closeTime(request.closeTime())
                .weekdayMask(request.weekdayMask())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .owner(owner)
                .build();

        return toResponse(storeRegistrationRepository.save(store), owner.getId());
    }

    /**
     * adminDongCode 우선 사용. 미지정 시 address geocoding (SK TMAP POI) 으로 행정동 자동 매핑.
     * 1) 응답의 adminDongCode 로 직접 조회
     * 2) 실패 시 응답의 adminDongName (동 이름) 으로 fallback 검색
     * 3) 둘 다 실패 시 400.
     */
    private AdminDong resolveAdminDong(String adminDongCode, String address) {
        if (StringUtils.hasText(adminDongCode)) {
            return dongneService.findAdminDongByCode(adminDongCode);
        }
        if (!StringUtils.hasText(address)) {
            throw new CustomException(
                    ErrorCode.INVALID_STORE_REQUEST, "주소 또는 adminDongCode 가 필요합니다.");
        }
        SkAddress sk;
        try {
            sk = skAddressClient.resolveRoadAddress(address);
        } catch (CustomException e) {
            log.warn(
                    "행정동 자동 매핑 — SK API 호출 실패. address={}, code={}, msg={}",
                    address,
                    e.getErrorCode(),
                    e.getMessage());
            throw new CustomException(
                    ErrorCode.INVALID_STORE_REQUEST,
                    "주소로 행정동을 찾지 못했습니다. adminDongCode 를 직접 입력해주세요.");
        }
        if (sk == null) {
            throw new CustomException(
                    ErrorCode.INVALID_STORE_REQUEST, "주소로 행정동을 찾지 못했습니다.");
        }
        if (StringUtils.hasText(sk.adminDongCode())) {
            return adminDongRepository
                    .findByAdminDongCode(sk.adminDongCode())
                    .orElseGet(() -> findByDongNameOrThrow(sk.adminDongName(), address));
        }
        return findByDongNameOrThrow(sk.adminDongName(), address);
    }

    private AdminDong findByDongNameOrThrow(String adminDongName, String address) {
        if (!StringUtils.hasText(adminDongName)) {
            throw new CustomException(
                    ErrorCode.INVALID_STORE_REQUEST,
                    "주소에서 행정동 이름을 추출하지 못했습니다. adminDongCode 를 직접 입력해주세요.");
        }
        return adminDongRepository
                .findFirstByAdminDongNameContainingOrderByIdAsc(adminDongName)
                .orElseThrow(
                        () -> {
                            log.warn(
                                    "행정동 자동 매핑 — 이름으로도 미발견. address={}, dongName={}",
                                    address,
                                    adminDongName);
                            return new CustomException(
                                    ErrorCode.INVALID_STORE_REQUEST,
                                    "주소(" + address + ") 의 행정동(" + adminDongName + ") 을 찾지 못했습니다.");
                        });
    }

    @Transactional(readOnly = true)
    public StoreRegistrationResponse getStore(Long storeId, Long userId) {
        return toResponse(findStore(storeId), userId);
    }

    @Transactional
    public StoreRegistrationResponse patchStore(
            Long storeId, Long currentUserId, StoreRegistrationUpdateRequest request) {
        Store store = findStore(storeId);
        ensureOwner(store, currentUserId);

        AdminDong newAdminDong =
                request.adminDongCode() == null
                        ? null
                        : dongneService.findAdminDongByCode(request.adminDongCode());

        store.patch(
                trimOrNull(request.name()),
                request.category(),
                trimOrNull(request.address()),
                trimOrNull(request.phoneNumber()),
                request.description(),
                request.openTime(),
                request.closeTime(),
                request.weekdayMask(),
                request.latitude(),
                request.longitude(),
                newAdminDong);

        return toResponse(store, currentUserId);
    }

    @Transactional
    public void deleteStore(Long storeId, Long currentUserId) {
        Store store = findStore(storeId);
        ensureOwner(store, currentUserId);

        boolean hasActive =
                groupOrderRepository
                        .findTopByStoreIdAndStatusOrderByIdDesc(storeId, GroupOrderStatus.OPEN)
                        .isPresent();
        if (hasActive) {
            throw new CustomException(ErrorCode.STORE_HAS_ACTIVE_GROUP_ORDER);
        }
        store.softDelete();
    }

    @Transactional(readOnly = true)
    public ShopDetailResponse getStoreDetail(Long storeId, Long currentUserId) {
        Store store = findStore(storeId);
        GroupOrder active =
                groupOrderRepository
                        .findTopByStoreIdAndStatusOrderByIdDesc(storeId, GroupOrderStatus.OPEN)
                        .orElse(null);
        List<Menu> menus = menuRepository.findAllByStoreId(storeId);
        List<StoreImage> galleryImages = storeImageRepository.findByStoreIdOrderBySortOrderAsc(storeId);
        boolean likedByCurrentUser =
                currentUserId != null && storeLikeService.isLikedByUser(storeId, currentUserId);
        LocalDateTime now = LocalDateTime.now();
        OrderFlow activeFlow = orderFlowRepository
                .findTopByStoreIdAndStatusInOrderByIdDesc(storeId, ORDER_FLOW_ACTIVE_STATUSES)
                .orElse(null);
        RecruitmentStatus recruitmentStatus =
                recruitmentStatusCalculator.calculate(store, activeFlow, now);
        return ShopDetailResponse.from(
                store,
                active,
                activeFlow,
                menus,
                galleryImages,
                likedByCurrentUser,
                recruitmentStatus);
    }

    @Transactional(readOnly = true)
    public PageResponse<ShopSummaryResponse> search(
            StoreSearchCriteria criteria, Pageable pageable, Long currentUserId) {
        if (Boolean.TRUE.equals(criteria.likedOnly()) && currentUserId == null) {
            return PageResponse.of(List.of(), pageable, 0);
        }

        Page<Store> page =
                storeRegistrationRepository.findAll(
                        StoreSpecifications.from(criteria, currentUserId), pageable);

        LocalDateTime now = LocalDateTime.now();
        List<Long> storeIds = page.getContent().stream().map(Store::getId).toList();
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

        Map<Long, OrderFlow> activeFlowByStoreId = fetchActiveFlowsByStoreIds(storeIds);

        List<ShopSummaryResponse> content =
                page.getContent().stream()
                        .map(
                                store -> {
                                    GroupOrder active = activeByStoreId.get(store.getId());
                                    OrderFlow activeFlow = activeFlowByStoreId.get(store.getId());
                                    return ShopSummaryResponse.from(
                                            store,
                                            active,
                                            storeLikeService.isLikedByUser(
                                                    store.getId(), currentUserId),
                                            recruitmentStatusCalculator.calculate(
                                                    store, activeFlow, now));
                                })
                        .toList();
        return PageResponse.from(page, content);
    }

    private Map<Long, OrderFlow> fetchActiveFlowsByStoreIds(List<Long> storeIds) {
        if (storeIds.isEmpty()) return Map.of();
        Map<Long, OrderFlow> result = new java.util.HashMap<>();
        for (Long sid : storeIds) {
            orderFlowRepository
                    .findTopByStoreIdAndStatusInOrderByIdDesc(sid, ORDER_FLOW_ACTIVE_STATUSES)
                    .ifPresent(f -> result.put(sid, f));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ShopSummaryResponse> getRandomStores(int size, Long currentUserId) {
        int capped = Math.min(Math.max(size, 1), 20);
        List<Store> stores = storeRegistrationRepository.findRandom(capped);
        if (stores.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Long> storeIds = stores.stream().map(Store::getId).toList();
        Map<Long, GroupOrder> activeByStoreId =
                groupOrderRepository.findByStoreIdInAndStatus(storeIds, GroupOrderStatus.OPEN).stream()
                        .collect(
                                Collectors.toMap(
                                        go -> go.getStore().getId(),
                                        Function.identity(),
                                        (a, b) -> a.getId() > b.getId() ? a : b));

        Map<Long, OrderFlow> activeFlowByStoreId = fetchActiveFlowsByStoreIds(storeIds);

        return stores.stream()
                .map(
                        store -> {
                            GroupOrder active = activeByStoreId.get(store.getId());
                            OrderFlow activeFlow = activeFlowByStoreId.get(store.getId());
                            return ShopSummaryResponse.from(
                                    store,
                                    active,
                                    storeLikeService.isLikedByUser(store.getId(), currentUserId),
                                    recruitmentStatusCalculator.calculate(
                                            store, activeFlow, now));
                        })
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<StoreRegistrationResponse> getMyStores(Long ownerId, Pageable pageable) {
        Page<Store> page =
                storeRegistrationRepository.findByOwnerIdOrderByIdDesc(ownerId, pageable);
        List<StoreRegistrationResponse> content =
                page.getContent().stream().map(s -> toResponse(s, ownerId)).toList();
        return PageResponse.from(page, content);
    }

    /** 가게 owner 검증. */
    public Store findOwnedStore(Long storeId, Long currentUserId) {
        Store store = findStore(storeId);
        ensureOwner(store, currentUserId);
        return store;
    }

    private Store findStore(Long storeId) {
        return storeRegistrationRepository
                .findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
    }

    private void ensureOwner(Store store, Long currentUserId) {
        if (currentUserId == null || !store.getOwner().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.STORE_FORBIDDEN);
        }
    }

    private String trimOrNull(String value) {
        return value == null ? null : value.trim();
    }

    private StoreRegistrationResponse toResponse(Store store, Long userId) {
        return StoreRegistrationResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .category(store.getCategory())
                .categoryLabel(store.getCategory() == null ? null : store.getCategory().getLabel())
                .address(store.getAddress())
                .phoneNumber(store.getPhoneNumber())
                .description(store.getDescription())
                .openTime(store.getOpenTime())
                .closeTime(store.getCloseTime())
                .weekdayMask(store.getWeekdayMask())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .thumbnailUrl(store.getThumbnailUrl())
                .likeCount(storeLikeService.getLikeCount(store.getId()))
                .likedByCurrentUser(storeLikeService.isLikedByUser(store.getId(), userId))
                .build();
    }
}
