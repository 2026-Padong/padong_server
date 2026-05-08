package com.example.padong_server.domain.picture.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.picture.dto.AdminDongPictureResponse;
import com.example.padong_server.domain.picture.entity.TourPicture;
import com.example.padong_server.domain.picture.repository.TourPictureRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PictureServiceTest {

    @Mock
    private DongneService dongneService;

    @Mock
    private TourPictureRepository tourPictureRepository;

    @InjectMocks
    private PictureService pictureService;

    @Test
    @DisplayName("행정동 코드로 저장된 관광 사진 목록을 조회한다")
    void getPicturesByAdminDongCode_returnsPictures() {
        AdminDong adminDong = adminDong("1168052100", "삼성1동");
        TourPicture firstPicture = tourPicture(
                "2456536",
                "강남 마이스 관광특구",
                "서울특별시 강남구 영동대로 513 (삼성동)",
                "http://example.com/image.jpg",
                "삼성1동",
                "1168052100"
        );
        TourPicture secondPicture = tourPicture(
                "3439947",
                "강남 미디어아트",
                "서울특별시 강남구 영동대로 511",
                "http://example.com/image2.jpg",
                "삼성1동",
                "1168052100"
        );

        given(dongneService.findAdminDongByCode("1168052100")).willReturn(adminDong);
        given(tourPictureRepository.findByAdminDongCodeOrderByTitleAscContentIdAsc("1168052100"))
                .willReturn(List.of(firstPicture, secondPicture));

        AdminDongPictureResponse response = pictureService.getPicturesByAdminDongCode("1168052100");

        assertThat(response.getAdminDongCode()).isEqualTo("1168052100");
        assertThat(response.getAdminDongName()).isEqualTo("삼성1동");
        assertThat(response.getPictures()).hasSize(2);
        assertThat(response.getPictures().get(0).getTitle()).isEqualTo("강남 마이스 관광특구");
        assertThat(response.getPictures().get(0).getFirstImageUrl()).isEqualTo("http://example.com/image.jpg");
        assertThat(response.getPictures().get(1).getRoadAddress()).isEqualTo("서울특별시 강남구 영동대로 511");
    }

    @Test
    @DisplayName("행정동에 저장된 사진이 없으면 예외를 던진다")
    void getPicturesByAdminDongCode_throwsWhenPictureMissing() {
        AdminDong adminDong = adminDong("1168052100", "삼성1동");

        given(dongneService.findAdminDongByCode("1168052100")).willReturn(adminDong);
        given(tourPictureRepository.findByAdminDongCodeOrderByTitleAscContentIdAsc("1168052100"))
                .willReturn(List.of());

        assertThatThrownBy(() -> pictureService.getPicturesByAdminDongCode("1168052100"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PICTURE_NOT_FOUND);
    }

    private AdminDong adminDong(String code, String name) {
        AdminDong adminDong = new AdminDong();
        ReflectionTestUtils.setField(adminDong, "adminDongCode", code);
        ReflectionTestUtils.setField(adminDong, "adminDongName", name);
        return adminDong;
    }

    private TourPicture tourPicture(
            String contentId,
            String title,
            String roadAddress,
            String firstImageUrl,
            String adminDongName,
            String adminDongCode
    ) {
        return TourPicture.builder()
                .contentId(contentId)
                .title(title)
                .roadAddress(roadAddress)
                .firstImageUrl(firstImageUrl)
                .adminDongName(adminDongName)
                .adminDongCode(adminDongCode)
                .build();
    }
}
