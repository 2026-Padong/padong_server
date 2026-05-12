package com.example.padong_server.domain.picture.controller;

import com.example.padong_server.domain.picture.dto.AdminDongPictureResponse;
import com.example.padong_server.domain.picture.dto.PictureImportResponse;
import com.example.padong_server.domain.picture.dto.PictureItemResponse;
import com.example.padong_server.domain.picture.dto.PictureMappingResponse;
import com.example.padong_server.domain.picture.service.PictureService;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.exception.GlobalExceptionHandler;
import java.util.List;
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

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("행정동 코드로 관광 사진 목록 응답을 반환한다")
    void getPicturesByAdminDongCode_returnsResponse() throws Exception {
        AdminDongPictureResponse response = AdminDongPictureResponse.builder()
                .adminDongCode("1168052100")
                .adminDongName("삼성1동")
                .pictures(List.of(
                        PictureItemResponse.builder()
                                .contentId("2456536")
                                .title("강남 마이스 관광특구")
                                .roadAddress("서울특별시 강남구 영동대로 513 (삼성동)")
                                .firstImageUrl("http://example.com/image.jpg")
                                .adminDongCode("1168052100")
                                .adminDongName("삼성1동")
                                .build()
                ))
                .build();

        given(pictureService.getPicturesByAdminDongCode("1168052100")).willReturn(response);

        mockMvc.perform(get("/pictures")
                        .param("adminDongCode", "1168052100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminDongCode").value("1168052100"))
                .andExpect(jsonPath("$.adminDongName").value("삼성1동"))
                .andExpect(jsonPath("$.pictures[0].title").value("강남 마이스 관광특구"))
                .andExpect(jsonPath("$.pictures[0].firstImageUrl").value("http://example.com/image.jpg"));
    }

    @Test
    @DisplayName("관광 사진이 없으면 공통 에러 응답을 반환한다")
    void getPicturesByAdminDongCode_returnsErrorResponse() throws Exception {
        given(pictureService.getPicturesByAdminDongCode("1168052100"))
                .willThrow(new CustomException(ErrorCode.PICTURE_NOT_FOUND));

        mockMvc.perform(get("/pictures")
                        .param("adminDongCode", "1168052100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PICTURE_NOT_FOUND"));
    }

    @Test
    @DisplayName("관광 정보 원본 적재 응답을 반환한다")
    void importPictures_returnsResponse() throws Exception {
        PictureImportResponse response = new PictureImportResponse(120, 90, 30, 2, 998);
        given(pictureService.importPictures()).willReturn(response);

        mockMvc.perform(post("/pictures/data")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fetchedContentCount").value(120))
                .andExpect(jsonPath("$.savedPictureCount").value(90))
                .andExpect(jsonPath("$.skippedNoImageCount").value(30))
                .andExpect(jsonPath("$.usedTourApiCallCount").value(2))
                .andExpect(jsonPath("$.remainingTourApiCallCount").value(998));
    }

    @Test
    @DisplayName("행정동 매핑 응답을 반환한다")
    void mapPicturesToAdminDong_returnsResponse() throws Exception {
        PictureMappingResponse response = new PictureMappingResponse(90L, 90, 80, 10, 50, 30, 0, 500, false);
        given(pictureService.mapPicturesToAdminDong(500, 0)).willReturn(response);

        mockMvc.perform(post("/pictures/mappings/admin-dong")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPictureCount").value(90))
                .andExpect(jsonPath("$.processedCount").value(90))
                .andExpect(jsonPath("$.mappedPictureCount").value(80))
                .andExpect(jsonPath("$.skippedUnresolvedCount").value(10))
                .andExpect(jsonPath("$.resolvedByParenthesisCount").value(50))
                .andExpect(jsonPath("$.resolvedByAddressApiCount").value(30))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.limit").value(500))
                .andExpect(jsonPath("$.hasNext").value(false));
    }
}
