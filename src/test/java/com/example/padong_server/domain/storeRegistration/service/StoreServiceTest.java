package com.example.padong_server.domain.storeRegistration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.storeLike.service.StoreLikeService;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.repository.StoreRegistrationRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRegistrationRepository storeRegistrationRepository;

    @Mock
    private StoreLikeService storeLikeService;

    private StoreRegistrationService storeRegistrationService;

    @BeforeEach
    void setUp() {
        storeRegistrationService = new StoreRegistrationService(storeRegistrationRepository, storeLikeService);
    }

    @Test
    @DisplayName("가게 상세 조회 시 총 좋아요 수와 내 좋아요 여부를 함께 반환한다")
    void getStore_returnsLikeMetadata() {
        Store store = Store.builder()
                .id(1L)
                .name("파동식당")
                .roadAddress("서울시 송파구")
                .phoneNumber("02-1234-5678")
                .operatingHours("10:00-20:00")
                .build();
        when(storeRegistrationRepository.findById(1L)).thenReturn(Optional.of(store));
        when(storeLikeService.getLikeCount(1L)).thenReturn(5L);
        when(storeLikeService.isLikedByUser(1L, 99L)).thenReturn(true);

        var response = storeRegistrationService.getStore(1L, 99L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getLikeCount()).isEqualTo(5L);
        assertThat(response.isLikedByCurrentUser()).isTrue();
    }
}
