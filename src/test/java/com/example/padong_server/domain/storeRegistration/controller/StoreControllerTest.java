package com.example.padong_server.domain.storeRegistration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.oauth.entity.Role;
import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.storeLike.dto.StoreLikeToggleResponse;
import com.example.padong_server.domain.storeLike.service.StoreLikeService;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationResponse;
import com.example.padong_server.domain.storeRegistration.service.StoreRegistrationService;
import com.example.padong_server.global.exception.GlobalExceptionHandler;
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
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getStore returns like fields")
    void getStore_returnsLikeFields() throws Exception {
        StoreRegistrationResponse response = StoreRegistrationResponse.builder()
                .id(1L)
                .name("Padong")
                .address("Seoul")
                .phoneNumber("02-1234-5678")
                .operatingHours("10:00-20:00")
                .likeCount(7L)
                .likedByCurrentUser(true)
                .build();
        given(storeRegistrationService.getStore(1L, 99L)).willReturn(response);

        mockMvc.perform(get("/stores")
                        .param("storeId", "1")
                        .param("userId", "99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(7))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(true));
    }

    @Test
    @DisplayName("createStore forwards authenticated owner")
    void createStore_usesAuthenticatedOwner() throws Exception {
        User user = authenticatedUser(11L);
        StoreRegistrationResponse response = StoreRegistrationResponse.builder()
                .id(5L)
                .name("Padong")
                .address("Seoul")
                .phoneNumber("02-9999-9999")
                .operatingHours("09:00-18:00")
                .likeCount(0L)
                .likedByCurrentUser(false)
                .build();
        given(storeRegistrationService.createStore(same(user), any())).willReturn(response);

        mockMvc.perform(post("/stores")
                        .param("name", "Padong")
                        .param("address", "Seoul")
                        .param("phoneNumber", "02-9999-9999")
                        .param("operatingHours", "09:00-18:00")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("Padong"));

        verify(storeRegistrationService).createStore(same(user), any());
    }

    @Test
    @DisplayName("toggleStoreLike returns current like state")
    void toggleStoreLike_returnsCurrentLikeState() throws Exception {
        StoreLikeToggleResponse response = StoreLikeToggleResponse.builder()
                .storeId(1L)
                .userId(99L)
                .liked(true)
                .likeCount(3L)
                .build();
        given(storeLikeService.toggleLike(1L, 99L)).willReturn(response);

        mockMvc.perform(post("/stores/likes")
                        .param("storeId", "1")
                        .param("userId", "99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(1))
                .andExpect(jsonPath("$.data.userId").value(99))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(3));
    }

    @Test
    @DisplayName("updateStore returns updated store")
    void updateStore_returnsUpdatedStore() throws Exception {
        StoreRegistrationResponse response = StoreRegistrationResponse.builder()
                .id(3L)
                .name("Updated Store")
                .address("New Address")
                .phoneNumber("02-2222-2222")
                .operatingHours("10:00-19:00")
                .likeCount(0L)
                .likedByCurrentUser(false)
                .build();
        given(storeRegistrationService.updateStore(eq(3L), any())).willReturn(response);

        mockMvc.perform(put("/stores")
                        .param("storeId", "3")
                        .param("name", "Updated Store")
                        .param("address", "New Address")
                        .param("phoneNumber", "02-2222-2222")
                        .param("operatingHours", "10:00-19:00")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.name").value("Updated Store"));

        verify(storeRegistrationService).updateStore(eq(3L), any());
    }

    @Test
    @DisplayName("deleteStore returns ok")
    void deleteStore_returnsOk() throws Exception {
        mockMvc.perform(delete("/stores")
                        .param("storeId", "4")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(storeRegistrationService).deleteStore(4L);
    }

    private User authenticatedUser(Long id) {
        User user = User.builder()
                .kakaoId(1000L + id)
                .email("store" + id + "@example.com")
                .nickname("store-user")
                .role(Role.USER)
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
