package com.example.padong_server.domain.storeRegistration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.storeLike.service.StoreLikeService;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationCreateRequest;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationUpdateRequest;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.repository.StoreRegistrationRepository;
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
    @DisplayName("getStore returns like metadata")
    void getStore_returnsLikeMetadata() {
        Store store = Store.builder()
                .id(1L)
                .name("Padong")
                .roadAddress("Seoul")
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

    @Test
    @DisplayName("createStore saves owner with request")
    void createStore_savesStoreFromRequest() {
        User owner = Mockito.mock(User.class);
        Store savedStore = Store.builder()
                .id(2L)
                .name("New Store")
                .roadAddress("Gangdong-gu")
                .phoneNumber("02-0000-0000")
                .operatingHours("09:00-18:00")
                .owner(owner)
                .build();
        when(storeRegistrationRepository.save(any(Store.class))).thenReturn(savedStore);
        when(storeLikeService.getLikeCount(2L)).thenReturn(0L);
        when(storeLikeService.isLikedByUser(2L, null)).thenReturn(false);

        var response = storeRegistrationService.createStore(owner, new StoreRegistrationCreateRequest(
                "New Store",
                "Gangdong-gu",
                "02-0000-0000",
                "09:00-18:00"
        ));

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getName()).isEqualTo("New Store");
        assertThat(response.getAddress()).isEqualTo("Gangdong-gu");
        verify(storeRegistrationRepository).save(argThat(store -> store.getOwner() == owner));
    }

    @Test
    @DisplayName("updateStore updates basic fields")
    void updateStore_updatesBasicFields() {
        Store store = Store.builder()
                .id(3L)
                .name("Old Store")
                .roadAddress("Old Address")
                .phoneNumber("02-1111-1111")
                .operatingHours("08:00-17:00")
                .build();
        when(storeRegistrationRepository.findById(3L)).thenReturn(Optional.of(store));
        when(storeLikeService.getLikeCount(3L)).thenReturn(2L);
        when(storeLikeService.isLikedByUser(3L, null)).thenReturn(false);

        var response = storeRegistrationService.updateStore(3L, new StoreRegistrationUpdateRequest(
                "Updated Store",
                "New Address",
                "02-2222-2222",
                "10:00-19:00"
        ));

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getName()).isEqualTo("Updated Store");
        assertThat(response.getAddress()).isEqualTo("New Address");
        assertThat(store.getPhoneNumber()).isEqualTo("02-2222-2222");
        assertThat(store.getOperatingHours()).isEqualTo("10:00-19:00");
    }

    @Test
    @DisplayName("deleteStore removes the store")
    void deleteStore_deletesFoundStore() {
        Store store = Store.builder()
                .id(4L)
                .name("Delete Store")
                .roadAddress("Delete Address")
                .phoneNumber("02-3333-3333")
                .operatingHours("10:00-18:00")
                .build();
        when(storeRegistrationRepository.findById(4L)).thenReturn(Optional.of(store));
        doNothing().when(storeRegistrationRepository).delete(store);

        storeRegistrationService.deleteStore(4L);

        verify(storeRegistrationRepository).delete(store);
    }
}
