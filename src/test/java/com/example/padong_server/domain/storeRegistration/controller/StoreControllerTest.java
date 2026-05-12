package com.example.padong_server.domain.storeRegistration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.oauth.entity.Role;
import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.storeLike.dto.StoreLikeToggleResponse;
import com.example.padong_server.domain.storeLike.service.StoreLikeService;
import com.example.padong_server.domain.storeRegistration.dto.ShopDetailResponse;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationResponse;
import com.example.padong_server.domain.storeRegistration.entity.StoreCategory;
import com.example.padong_server.domain.storeRegistration.service.StoreImageService;
import com.example.padong_server.domain.storeRegistration.service.StoreRegistrationService;
import com.example.padong_server.global.exception.GlobalExceptionHandler;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StoreControllerTest {

    @Mock private StoreRegistrationService storeRegistrationService;
    @Mock private StoreLikeService storeLikeService;
    @Mock private StoreImageService storeImageService;

    @InjectMocks private StoreRegistrationController storeRegistrationController;

    private MockMvc mockMvc;
    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(storeRegistrationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /stores/{id} 가 ShopDetailResponse 반환")
    void getStoreDetail_returnsResponse() throws Exception {
        authenticatedUser(7L);
        ShopDetailResponse response = new ShopDetailResponse(
                1L,
                "Padong",
                StoreCategory.BAKERY,
                "베이커리",
                null,
                true,
                "동네 빵집",
                "Seoul",
                "02-1234-5678",
                "10:00",
                "20:00",
                127,
                List.of(),
                List.of(),
                0,
                null,
                null,
                null,
                null,
                com.example.padong_server.domain.storeRegistration.entity.RecruitmentStatus.NO_FLOW);
        given(storeRegistrationService.getStoreDetail(1L, 7L)).willReturn(response);

        mockMvc.perform(get("/stores/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Padong"))
                .andExpect(jsonPath("$.data.category").value("BAKERY"))
                .andExpect(jsonPath("$.data.categoryLabel").value("베이커리"))
                .andExpect(jsonPath("$.data.weekdayMask").value(127))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(true));
    }

    @Test
    @DisplayName("POST /stores: JSON body 로 등록")
    void createStore_acceptsJsonBody() throws Exception {
        User user = authenticatedUser(11L);
        StoreRegistrationResponse response = StoreRegistrationResponse.builder()
                .id(5L)
                .name("Padong")
                .category(StoreCategory.BAKERY)
                .categoryLabel("베이커리")
                .address("Seoul")
                .phoneNumber("02-9999-9999")
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .weekdayMask(62)
                .likeCount(0L)
                .likedByCurrentUser(false)
                .build();
        given(storeRegistrationService.createStore(same(user), any())).willReturn(response);

        String body = MAPPER.writeValueAsString(
                java.util.Map.of(
                        "adminDongCode", "1162069500",
                        "name", "Padong",
                        "category", "BAKERY",
                        "address", "Seoul",
                        "phoneNumber", "02-9999-9999",
                        "openTime", "09:00",
                        "closeTime", "18:00",
                        "weekdayMask", 62));

        mockMvc.perform(post("/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("Padong"))
                .andExpect(jsonPath("$.data.category").value("BAKERY"));

        verify(storeRegistrationService).createStore(same(user), any());
    }

    @Test
    @DisplayName("POST /stores/likes: JWT userId 사용")
    void toggleStoreLike_returnsCurrentLikeState() throws Exception {
        authenticatedUser(99L);
        StoreLikeToggleResponse response = StoreLikeToggleResponse.builder()
                .storeId(1L)
                .userId(99L)
                .liked(true)
                .likeCount(3L)
                .build();
        given(storeLikeService.toggleLike(1L, 99L)).willReturn(response);

        mockMvc.perform(post("/stores/likes")
                        .param("storeId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(1))
                .andExpect(jsonPath("$.data.liked").value(true));
    }

    @Test
    @DisplayName("PATCH /stores/{id}: 부분 수정 반환")
    void patchStore_returnsUpdatedStore() throws Exception {
        authenticatedUser(11L);
        StoreRegistrationResponse response = StoreRegistrationResponse.builder()
                .id(3L)
                .name("Updated")
                .address("New Address")
                .build();
        given(storeRegistrationService.patchStore(eq(3L), eq(11L), any())).willReturn(response);

        String body = MAPPER.writeValueAsString(java.util.Map.of("name", "Updated"));

        mockMvc.perform(patch("/stores/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated"));

        verify(storeRegistrationService).patchStore(eq(3L), eq(11L), any());
    }

    @Test
    @DisplayName("DELETE /stores/{id}: 204 No Content")
    void deleteStore_returnsNoContent() throws Exception {
        authenticatedUser(11L);
        mockMvc.perform(delete("/stores/4")).andExpect(status().isNoContent());
        verify(storeRegistrationService).deleteStore(4L, 11L);
    }

    private User authenticatedUser(Long id) {
        User user = User.builder()
                .kakaoId(1000L + id)
                .email("store" + id + "@example.com")
                .nickname("store-user")
                .role(Role.ADMIN)
                .registered(true)
                .approved(true)
                .build();
        ReflectionTestUtils.setField(user, "id", id);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return user;
    }
}
