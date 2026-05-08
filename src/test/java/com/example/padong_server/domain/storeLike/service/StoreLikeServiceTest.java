package com.example.padong_server.domain.storeLike.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.storeLike.dto.StoreLikeToggleResponse;
import com.example.padong_server.domain.storeLike.entity.StoreLike;
import com.example.padong_server.domain.storeLike.repository.StoreLikeRepository;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.repository.StoreRegistrationRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreLikeServiceTest {

    @Mock
    private StoreLikeRepository storeLikeRepository;

    @Mock
    private StoreRegistrationRepository storeRegistrationRepository;

    private StoreLikeService storeLikeService;

    @BeforeEach
    void setUp() {
        storeLikeService = new StoreLikeService(storeLikeRepository, storeRegistrationRepository);
    }

    @Test
    @DisplayName("좋아요가 없으면 새로 추가하고 좋아요 수를 올린다")
    void toggleLike_createsLikeWhenMissing() {
        Store store = store(1L);
        when(storeRegistrationRepository.findById(1L)).thenReturn(Optional.of(store));
        when(storeLikeRepository.findByStoreRegistrationIdAndUserId(1L, 10L)).thenReturn(Optional.empty());
        when(storeLikeRepository.countByStoreRegistrationId(1L)).thenReturn(1L);

        StoreLikeToggleResponse response = storeLikeService.toggleLike(1L, 10L);

        assertThat(response.storeId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1L);
        verify(storeLikeRepository).save(any(StoreLike.class));
    }

    @Test
    @DisplayName("이미 좋아요가 있으면 삭제하고 좋아요 수를 내린다")
    void toggleLike_removesLikeWhenAlreadyExists() {
        StoreLike storeLike = StoreLike.builder()
                .id(7L)
                .store(store(1L))
                .userId(10L)
                .build();
        when(storeRegistrationRepository.findById(1L)).thenReturn(Optional.of(store(1L)));
        when(storeLikeRepository.findByStoreRegistrationIdAndUserId(1L, 10L)).thenReturn(Optional.of(storeLike));
        when(storeLikeRepository.countByStoreRegistrationId(1L)).thenReturn(0L);

        StoreLikeToggleResponse response = storeLikeService.toggleLike(1L, 10L);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isZero();
        verify(storeLikeRepository).delete(storeLike);
        verify(storeLikeRepository, never()).save(any(StoreLike.class));
    }

    @Test
    @DisplayName("userId 없이 상세 조회하면 본인 좋아요 여부는 false다")
    void isLikedByUser_returnsFalseWhenUserIdIsNull() {
        boolean liked = storeLikeService.isLikedByUser(1L, null);

        assertThat(liked).isFalse();
        verify(storeLikeRepository, never()).existsByStoreRegistrationIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("잘못된 userId로 좋아요 요청하면 예외가 난다")
    void toggleLike_throwsWhenUserIdIsInvalid() {
        assertThatThrownBy(() -> storeLikeService.toggleLike(1L, 0L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STORE_LIKE_REQUEST);
    }

    private Store store(Long id) {
        return Store.builder()
                .id(id)
                .name("테스트 가게")
                .roadAddress("서울시 강남구")
                .phoneNumber("010-0000-0000")
                .operatingHours("09:00-18:00")
                .build();
    }
}
