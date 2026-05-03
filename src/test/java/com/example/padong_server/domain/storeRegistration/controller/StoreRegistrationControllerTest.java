package com.example.padong_server.domain.storeRegistration.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.padong_server.domain.storeLike.dto.StoreLikeToggleResponse;
import com.example.padong_server.domain.storeLike.service.StoreLikeService;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationResponse;
import com.example.padong_server.domain.storeRegistration.service.StoreRegistrationService;
import com.example.padong_server.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StoreRegistrationControllerTest {

    @Mock
    private StoreRegistrationService storeRegistrationService;

    @Mock
    private StoreLikeService storeLikeService;

    @InjectMocks
    private StoreRegistrationController storeRegistrationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(storeRegistrationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("가게 상세 조회에 좋아요 정보가 포함된다")
    void getStore_returnsLikeFields() throws Exception {
        StoreRegistrationResponse response = StoreRegistrationResponse.builder()
                .id(1L)
                .name("파동식당")
                .address("서울시 송파구")
                .phoneNumber("02-1234-5678")
                .operatingHours("10:00-20:00")
                .likeCount(7L)
                .likedByCurrentUser(true)
                .build();
        given(storeRegistrationService.getStore(1L, 99L)).willReturn(response);

        mockMvc.perform(get("/api/stores")
                        .param("storeId", "1")
                        .param("userId", "99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(7))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(true));
    }

    @Test
    @DisplayName("좋아요 토글 API가 현재 상태와 총 개수를 반환한다")
    void toggleStoreLike_returnsCurrentLikeState() throws Exception {
        StoreLikeToggleResponse response = StoreLikeToggleResponse.builder()
                .storeId(1L)
                .userId(99L)
                .liked(true)
                .likeCount(3L)
                .build();
        given(storeLikeService.toggleLike(1L, 99L)).willReturn(response);

        mockMvc.perform(post("/api/stores/likes")
                        .param("storeId", "1")
                        .param("userId", "99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(1))
                .andExpect(jsonPath("$.data.userId").value(99))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(3));
    }
}
