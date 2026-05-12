package com.example.padong_server.domain.storeRegistration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.payment.entity.GroupOrderStatus;
import com.example.padong_server.domain.storeLike.service.StoreLikeService;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationCreateRequest;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationUpdateRequest;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.entity.StoreCategory;
import com.example.padong_server.domain.storeRegistration.repository.StoreImageRepository;
import com.example.padong_server.domain.storeRegistration.repository.StoreRegistrationRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock private StoreRegistrationRepository storeRegistrationRepository;
    @Mock private StoreLikeService storeLikeService;
    @Mock
    private com.example.padong_server.domain.payment.repository.GroupOrderRepository groupOrderRepository;
    @Mock private com.example.padong_server.domain.menu.repository.MenuRepository menuRepository;
    @Mock private StoreImageRepository storeImageRepository;
    @Mock private DongneService dongneService;
    @Mock private com.example.padong_server.domain.dongne.repository.AdminDongRepository adminDongRepository;
    @Mock private com.example.padong_server.global.client.sk.SkAddressClient skAddressClient;
    @Mock private com.example.padong_server.domain.orderFlow.repository.OrderFlowRepository orderFlowRepository;
    @Mock private com.example.padong_server.domain.storeRegistration.service.RecruitmentStatusCalculator recruitmentStatusCalculator;

    private StoreRegistrationService storeRegistrationService;

    @BeforeEach
    void setUp() {
        storeRegistrationService =
                new StoreRegistrationService(
                        storeRegistrationRepository,
                        storeLikeService,
                        groupOrderRepository,
                        menuRepository,
                        storeImageRepository,
                        dongneService,
                        adminDongRepository,
                        skAddressClient,
                        orderFlowRepository,
                        recruitmentStatusCalculator);
    }

    @Test
    @DisplayName("createStore: adminDong 매핑 + 풀 필드 빌더 호출")
    void createStore_savesStoreFromRequest() {
        User owner = Mockito.mock(User.class);
        when(owner.getId()).thenReturn(7L);
        AdminDong dong = Mockito.mock(AdminDong.class);
        when(dongneService.findAdminDongByCode("1162069500")).thenReturn(dong);

        Store saved = Store.builder()
                .id(2L)
                .adminDong(dong)
                .name("New Store")
                .category(StoreCategory.BAKERY)
                .address("Gangdong-gu")
                .phoneNumber("02-0000-0000")
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .weekdayMask(62)
                .owner(owner)
                .build();
        when(storeRegistrationRepository.save(any(Store.class))).thenReturn(saved);
        when(storeLikeService.getLikeCount(2L)).thenReturn(0L);
        when(storeLikeService.isLikedByUser(2L, 7L)).thenReturn(false);

        var response =
                storeRegistrationService.createStore(
                        owner,
                        new StoreRegistrationCreateRequest(
                                "1162069500",
                                "New Store",
                                StoreCategory.BAKERY,
                                "Gangdong-gu",
                                "02-0000-0000",
                                "한 줄 소개",
                                LocalTime.of(9, 0),
                                LocalTime.of(18, 0),
                                62,
                                null,
                                null));

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getCategory()).isEqualTo(StoreCategory.BAKERY);
        assertThat(response.getWeekdayMask()).isEqualTo(62);
    }

    @Test
    @DisplayName("patchStore: 본인 가게가 아니면 STORE_FORBIDDEN")
    void patchStore_rejectsNonOwner() {
        User owner = Mockito.mock(User.class);
        when(owner.getId()).thenReturn(7L);
        Store store = Store.builder()
                .id(3L)
                .name("Old")
                .address("Old Address")
                .phoneNumber("02-1111-1111")
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(17, 0))
                .owner(owner)
                .build();
        when(storeRegistrationRepository.findById(3L)).thenReturn(Optional.of(store));

        StoreRegistrationUpdateRequest request =
                new StoreRegistrationUpdateRequest(
                        null, "New Name", null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> storeRegistrationService.patchStore(3L, 99L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_FORBIDDEN);
    }

    @Test
    @DisplayName("deleteStore: active GroupOrder 있으면 409")
    void deleteStore_blocksWhenActiveGroupOrder() {
        User owner = Mockito.mock(User.class);
        when(owner.getId()).thenReturn(7L);
        Store store = Store.builder()
                .id(4L)
                .name("Delete")
                .address("Addr")
                .phoneNumber("02-3333-3333")
                .openTime(LocalTime.of(10, 0))
                .closeTime(LocalTime.of(18, 0))
                .owner(owner)
                .build();
        when(storeRegistrationRepository.findById(4L)).thenReturn(Optional.of(store));
        when(groupOrderRepository.findTopByStoreIdAndStatusOrderByIdDesc(
                        eq(4L), eq(GroupOrderStatus.OPEN)))
                .thenReturn(Optional.of(Mockito.mock(com.example.padong_server.domain.payment.entity.GroupOrder.class)));

        assertThatThrownBy(() -> storeRegistrationService.deleteStore(4L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_HAS_ACTIVE_GROUP_ORDER);
    }

    @Test
    @DisplayName("deleteStore: 본인 가게이고 active 공구 없으면 softDelete")
    void deleteStore_softDeletesWhenNoActive() {
        User owner = Mockito.mock(User.class);
        when(owner.getId()).thenReturn(7L);
        Store store = Store.builder()
                .id(5L)
                .name("X")
                .address("X")
                .phoneNumber("02")
                .openTime(LocalTime.of(10, 0))
                .closeTime(LocalTime.of(18, 0))
                .owner(owner)
                .build();
        when(storeRegistrationRepository.findById(5L)).thenReturn(Optional.of(store));
        when(groupOrderRepository.findTopByStoreIdAndStatusOrderByIdDesc(
                        eq(5L), eq(GroupOrderStatus.OPEN)))
                .thenReturn(Optional.empty());

        storeRegistrationService.deleteStore(5L, 7L);

        assertThat(store.isDeleted()).isTrue();
    }
}
