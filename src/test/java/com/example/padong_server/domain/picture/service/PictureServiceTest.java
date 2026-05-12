package com.example.padong_server.domain.picture.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.picture.dto.AdminDongPictureResponse;
import com.example.padong_server.domain.picture.dto.PictureImportResponse;
import com.example.padong_server.domain.picture.dto.PictureMappingResponse;
import com.example.padong_server.domain.picture.entity.TourPicture;
import com.example.padong_server.domain.picture.repository.TourPictureRepository;
import com.example.padong_server.global.client.sk.SkAddress;
import com.example.padong_server.global.client.sk.SkAddressClient;
import com.example.padong_server.global.client.tour.TourApiClient;
import com.example.padong_server.global.client.tour.TourApiQuotaService;
import com.example.padong_server.global.client.tour.TourContent;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PictureServiceTest {

    @Mock
    private TourApiClient tourApiClient;

    @Mock
    private TourApiQuotaService tourApiQuotaService;

    @Mock
    private SkAddressClient skAddressClient;

    @Mock
    private DongneService dongneService;

    @Mock
    private AdminDongRepository adminDongRepository;

    @Mock
    private TourPictureRepository tourPictureRepository;

    @InjectMocks
    private PictureService pictureService;

    @Test
    @DisplayName("행정동 코드로 등록된 관광 사진 목록을 조회한다")
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
    @DisplayName("행정동에 등록된 관광 사진이 없으면 예외를 던진다")
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

    @Test
    @DisplayName("관광 정보 원본을 적재하고 기존 행정동 매핑은 비운다")
    void importPictures_importsRawPicturesAndClearsMapping() {
        LocalDate today = LocalDate.now();
        TourPicture existingPicture = tourPicture(
                "100",
                "예전 제목",
                "서울시 종로구 예전로 1",
                "http://example.com/old.jpg",
                "청운효자동",
                "1111051500"
        );

        TourContent firstContent = new TourContent(
                "100",
                "새 제목",
                "서울시 종로구 새문안로 1",
                "http://example.com/new.jpg",
                "http://example.com/new2.jpg"
        );
        TourContent secondContent = new TourContent(
                "200",
                "두 번째 관광지",
                "서울시 종로구 세종대로 1",
                "http://example.com/second.jpg",
                ""
        );
        TourContent noImageContent = new TourContent(
                "300",
                "이미지 없음",
                "서울시 종로구 자하문로 1",
                "",
                ""
        );

        given(tourApiQuotaService.getRemainingCalls(today)).willReturn(5);
        given(tourApiClient.getSeoulContents(5))
                .willReturn(new TourApiClient.TourContentPageResult(
                        List.of(firstContent, secondContent, noImageContent),
                        2
                ));
        given(tourApiQuotaService.getUsedCalls(today)).willReturn(2);
        given(tourPictureRepository.findByContentId("100")).willReturn(Optional.of(existingPicture));
        given(tourPictureRepository.findByContentId("200")).willReturn(Optional.empty());

        PictureImportResponse response = pictureService.importPictures();

        assertThat(response.fetchedContentCount()).isEqualTo(3);
        assertThat(response.savedPictureCount()).isEqualTo(2);
        assertThat(response.skippedNoImageCount()).isEqualTo(1);
        assertThat(response.usedTourApiCallCount()).isEqualTo(2);
        assertThat(response.remainingTourApiCallCount()).isEqualTo(TourApiQuotaService.DAILY_LIMIT - 2);

        assertThat(existingPicture.getTitle()).isEqualTo("새 제목");
        assertThat(existingPicture.getRoadAddress()).isEqualTo("서울시 종로구 새문안로 1");
        assertThat(existingPicture.getFirstImageUrl()).isEqualTo("http://example.com/new.jpg");
        assertThat(existingPicture.getAdminDongName()).isEmpty();
        assertThat(existingPicture.getAdminDongCode()).isEmpty();

        verify(tourPictureRepository).save(existingPicture);
        verify(tourPictureRepository).save(argThat(picture ->
                picture.getContentId().equals("200")
                        && picture.getTitle().equals("두 번째 관광지")
                        && picture.getRoadAddress().equals("서울시 종로구 세종대로 1")
                        && picture.getFirstImageUrl().equals("http://example.com/second.jpg")
                        && picture.getAdminDongName().isEmpty()
                        && picture.getAdminDongCode().isEmpty()
        ));
    }

    @Test
    @DisplayName("저장된 관광 정보에 행정동 매핑을 수행한다")
    void mapPicturesToAdminDong_mapsStoredPictures() {
        TourPicture parenthesizedPicture = tourPicture(
                "100",
                "경복궁",
                "서울시 종로구 사직로 161 (세종로)",
                "http://example.com/a.jpg",
                null,
                null
        );
        TourPicture apiResolvedPicture = tourPicture(
                "200",
                "통인시장",
                "서울시 종로구 자하문로15길 18",
                "http://example.com/b.jpg",
                null,
                null
        );
        TourPicture unresolvedPicture = tourPicture(
                "300",
                "미확인 장소",
                "서울시 어딘가 알수없음 1",
                "http://example.com/c.jpg",
                "기존동",
                "9999999999"
        );

        AdminDong sejongDong = adminDong("1111051500", "청운효자동");
        AdminDong sajikDong = adminDong("1111053000", "사직동");

        given(tourPictureRepository.count()).willReturn(3L);
        given(tourPictureRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .willReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(parenthesizedPicture, apiResolvedPicture, unresolvedPicture)));
        given(adminDongRepository.findFirstByAdminDongNameContainingOrderByIdAsc("세종로"))
                .willReturn(Optional.of(sejongDong));
        given(skAddressClient.resolveRoadAddress("서울시 종로구 자하문로15길 18"))
                .willReturn(new SkAddress(
                        "서울시 종로구 자하문로15길 18",
                        "서울시 종로구 사직동 1-1",
                        "사직동",
                        "1111053000"
                ));
        given(adminDongRepository.findByAdminDongCode("1111053000")).willReturn(Optional.of(sajikDong));
        given(skAddressClient.resolveRoadAddress("서울시 어딘가 알수없음 1"))
                .willThrow(new CustomException(ErrorCode.ADDRESS_API_CALL_FAILED));

        PictureMappingResponse response = pictureService.mapPicturesToAdminDong(500, 0);

        assertThat(response.totalPictureCount()).isEqualTo(3L);
        assertThat(response.processedCount()).isEqualTo(3);
        assertThat(response.mappedPictureCount()).isEqualTo(2);
        assertThat(response.skippedUnresolvedCount()).isEqualTo(1);
        assertThat(response.resolvedByParenthesisCount()).isEqualTo(1);
        assertThat(response.resolvedByAddressApiCount()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();

        assertThat(parenthesizedPicture.getAdminDongCode()).isEqualTo("1111051500");
        assertThat(parenthesizedPicture.getAdminDongName()).isEqualTo("청운효자동");
        assertThat(apiResolvedPicture.getAdminDongCode()).isEqualTo("1111053000");
        assertThat(apiResolvedPicture.getAdminDongName()).isEqualTo("사직동");
        assertThat(unresolvedPicture.getAdminDongCode()).isEmpty();
        assertThat(unresolvedPicture.getAdminDongName()).isEmpty();
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
