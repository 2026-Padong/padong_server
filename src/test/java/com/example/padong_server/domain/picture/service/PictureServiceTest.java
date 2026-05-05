package com.example.padong_server.domain.picture.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.entity.DongMapping;
import com.example.padong_server.domain.dongne.entity.LegalDong;
import com.example.padong_server.domain.dongne.repository.DongMappingRepository;
import com.example.padong_server.domain.dongne.repository.LegalDongRepository;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.picture.dto.AdminDongPictureResponse;
import com.example.padong_server.global.client.juso.JusoAddress;
import com.example.padong_server.global.client.juso.JusoAddressClient;
import com.example.padong_server.global.client.tour.TourApiClient;
import com.example.padong_server.global.client.tour.TourContent;
import com.example.padong_server.global.client.tour.TourImage;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PictureServiceTest {

    @Mock
    private DongneService dongneService;

    @Mock
    private DongMappingRepository dongMappingRepository;

    @Mock
    private LegalDongRepository legalDongRepository;

    @Mock
    private JusoAddressClient jusoAddressClient;

    @Mock
    private TourApiClient tourApiClient;

    @InjectMocks
    private PictureService pictureService;

    @Test
    @DisplayName("행정동 코드로 TourAPI 사진 목록을 조회한다")
    void getPicturesByAdminDongCode_returnsPictures() {
        AdminDong adminDong = adminDong("1168052100", "삼성1동");
        LegalDong legalDong = legalDong("1168010500", "삼성동", "서울특별시", "강남구");
        TourContent directMatch = new TourContent(
                "2456536",
                "강남 마이스 관광특구",
                "서울특별시 강남구 영동대로 513 (삼성동)",
                "http://example.com/fallback.jpg",
                "http://example.com/fallback-thumb.jpg"
        );
        TourContent apiResolved = new TourContent(
                "3439947",
                "강남 미디어 아트",
                "서울특별시 강남구 영동대로 511",
                "http://example.com/fallback2.jpg",
                "http://example.com/fallback2-thumb.jpg"
        );

        given(dongneService.findAdminDongByCode("1168052100")).willReturn(adminDong);
        given(dongMappingRepository.findByAdminDong(adminDong)).willReturn(List.of(mapping(adminDong, legalDong)));
        given(tourApiClient.getSeoulContents()).willReturn(List.of(directMatch, apiResolved));
        given(legalDongRepository.findByCityNameAndDistrictNameAndLegalDongName("서울특별시", "강남구", "삼성동"))
                .willReturn(Optional.of(legalDong));
        given(tourApiClient.getDetailImages("2456536"))
                .willReturn(List.of(new TourImage("museum", "http://example.com/image.jpg", "http://example.com/thumb.jpg")));
        given(jusoAddressClient.resolveRoadAddress("서울특별시 강남구 영동대로 511"))
                .willReturn(new JusoAddress(
                        "서울특별시 강남구 영동대로 511",
                        "서울특별시 강남구 영동대로 511",
                        "서울특별시 강남구 삼성동 159",
                        "1168010500",
                        "서울특별시",
                        "강남구",
                        "삼성동"
                ));
        given(tourApiClient.getDetailImages("3439947")).willReturn(List.of());

        AdminDongPictureResponse response = pictureService.getPicturesByAdminDongCode("1168052100");

        assertThat(response.getAdminDongCode()).isEqualTo("1168052100");
        assertThat(response.getAdminDongName()).isEqualTo("삼성1동");
        assertThat(response.getPictureCount()).isEqualTo(2);
        assertThat(response.getPictures()).hasSize(2);
        assertThat(response.getPictures().get(0).getImageUrl()).isEqualTo("http://example.com/image.jpg");
        assertThat(response.getPictures().get(1).getJibunAddress()).isEqualTo("서울특별시 강남구 삼성동 159");
    }

    @Test
    @DisplayName("괄호 안 법정동이 있으면 주소 API 없이 매칭한다")
    void getPicturesByAdminDongCode_resolvesByParenthesizedLegalDong() {
        AdminDong adminDong = adminDong("1168052100", "삼성1동");
        LegalDong legalDong = legalDong("1168010500", "삼성동", "서울특별시", "강남구");
        TourContent parenthesized = new TourContent(
                "2456536",
                "강남 마이스 관광특구",
                "서울특별시 강남구 영동대로 513 (삼성동)",
                "http://example.com/fallback.jpg",
                "http://example.com/fallback-thumb.jpg"
        );

        given(dongneService.findAdminDongByCode("1168052100")).willReturn(adminDong);
        given(dongMappingRepository.findByAdminDong(adminDong)).willReturn(List.of(mapping(adminDong, legalDong)));
        given(tourApiClient.getSeoulContents()).willReturn(List.of(parenthesized));
        given(legalDongRepository.findByCityNameAndDistrictNameAndLegalDongName("서울특별시", "강남구", "삼성동"))
                .willReturn(Optional.of(legalDong));
        given(tourApiClient.getDetailImages("2456536")).willReturn(List.of());

        AdminDongPictureResponse response = pictureService.getPicturesByAdminDongCode("1168052100");

        assertThat(response.getPictureCount()).isEqualTo(1);
        verify(jusoAddressClient, never()).resolveRoadAddress(parenthesized.address());
    }

    @Test
    @DisplayName("매칭되는 사진이 없으면 예외를 던진다")
    void getPicturesByAdminDongCode_throwsWhenPictureMissing() {
        AdminDong adminDong = adminDong("1168052100", "삼성1동");
        LegalDong legalDong = legalDong("1168010500", "삼성동", "서울특별시", "강남구");

        given(dongneService.findAdminDongByCode("1168052100")).willReturn(adminDong);
        given(dongMappingRepository.findByAdminDong(adminDong)).willReturn(List.of(mapping(adminDong, legalDong)));
        given(tourApiClient.getSeoulContents()).willReturn(List.of());

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

    private LegalDong legalDong(String code, String name, String cityName, String districtName) {
        LegalDong legalDong = new LegalDong();
        ReflectionTestUtils.setField(legalDong, "legalDongCode", code);
        ReflectionTestUtils.setField(legalDong, "legalDongName", name);
        ReflectionTestUtils.setField(legalDong, "cityName", cityName);
        ReflectionTestUtils.setField(legalDong, "districtName", districtName);
        return legalDong;
    }

    private DongMapping mapping(AdminDong adminDong, LegalDong legalDong) {
        DongMapping mapping = new DongMapping();
        ReflectionTestUtils.setField(mapping, "adminDong", adminDong);
        ReflectionTestUtils.setField(mapping, "legalDong", legalDong);
        return mapping;
    }
}
