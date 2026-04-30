package com.example.padong_server.domain.picture.controller;

import com.example.padong_server.domain.picture.dto.AdminDongPictureResponse;
import com.example.padong_server.domain.picture.dto.PictureItemResponse;
import com.example.padong_server.domain.picture.service.PictureService;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PictureControllerTest {

    @Mock
    private PictureService pictureService;

    @InjectMocks
    private PictureController pictureController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pictureController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("행정동 코드로 사진 목록을 반환한다")
    void getPicturesByAdminDongCode_returnsResponse() throws Exception {
        AdminDongPictureResponse response = AdminDongPictureResponse.builder()
                .adminDongCode("1168052100")
                .adminDongName("삼성1동")
                .pictureCount(1)
                .pictures(List.of(
                        PictureItemResponse.builder()
                                .contentId("2456536")
                                .title("강남 마이스 관광특구")
                                .roadAddress("서울특별시 강남구 영동대로 513 (삼성동)")
                                .jibunAddress("서울특별시 강남구 삼성동 159")
                                .imageUrl("http://example.com/image.jpg")
                                .thumbnailUrl("http://example.com/thumb.jpg")
                                .legalDongCode("1168010500")
                                .legalDongName("삼성동")
                                .build()
                ))
                .build();

        given(pictureService.getPicturesByAdminDongCode("1168052100")).willReturn(response);

        mockMvc.perform(get("/api/pictures/admin-dongs/1168052100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminDongCode").value("1168052100"))
                .andExpect(jsonPath("$.adminDongName").value("삼성1동"))
                .andExpect(jsonPath("$.pictureCount").value(1))
                .andExpect(jsonPath("$.pictures[0].title").value("강남 마이스 관광특구"));
    }

    @Test
    @DisplayName("사진이 없으면 공통 에러 응답을 반환한다")
    void getPicturesByAdminDongCode_returnsErrorResponse() throws Exception {
        given(pictureService.getPicturesByAdminDongCode("1168052100"))
                .willThrow(new CustomException(ErrorCode.PICTURE_NOT_FOUND));

        mockMvc.perform(get("/api/pictures/admin-dongs/1168052100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PICTURE_NOT_FOUND"));
    }
}
