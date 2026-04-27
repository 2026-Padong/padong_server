package com.example.padong_server.domain.hotplace.service;

import com.example.padong_server.domain.hotplace.dto.DistrictRealtimeResponse;
import com.example.padong_server.domain.hotplace.entity.Category;
import com.example.padong_server.domain.hotplace.entity.HotPlace;
import com.example.padong_server.domain.hotplace.repository.HotPlaceRepository;
import com.example.padong_server.global.client.seoul.SeoulRealtimeClient;
import com.example.padong_server.global.client.seoul.SeoulRealtimeData;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HotplaceRealtimeServiceTest {

    @Mock
    private HotPlaceRepository hotPlaceRepository;

    @Mock
    private SeoulRealtimeClient seoulRealtimeClient;

    @InjectMocks
    private HotplaceRealtimeService hotplaceRealtimeService;

    @Test
    @DisplayName("guName으로 조회하면 요약 정보와 핫플레이스 목록을 반환한다")
    void getDistrictRealtime_returnsSummaryAndHotplaces() {
        HotPlace museum = hotPlace("\uC6A9\uC0B0\uAD6C", "\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00", Category.CULTURAL_HERITAGE);
        HotPlace park = hotPlace("\uC6A9\uC0B0\uAD6C", "\uC6A9\uC0B0\uACF5\uC6D0", Category.PARK);

        given(hotPlaceRepository.findByGuName("\uC6A9\uC0B0\uAD6C")).willReturn(List.of(museum, park));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00"))
                .willReturn(realtimeData("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00", "\uC5EC\uC720",
                        "\uC0AC\uB78C\uC774 \uBAB0\uB824\uC788\uC744 \uAC00\uB2A5\uC131\uC774 \uB0AE\uACE0 \uBD90\uBE54\uC740 \uAC70\uC758 \uB290\uAEF4\uC9C0\uC9C0 \uC54A\uC544\uC694.",
                        "\uB9D1\uC74C", 21.3, 25.0, 10.0));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("\uC6A9\uC0B0\uACF5\uC6D0"))
                .willReturn(realtimeData("\uC6A9\uC0B0\uACF5\uC6D0", "\uBCF4\uD1B5",
                        "\uC77C\uBD80 \uAD6C\uAC04\uC774 \uD63C\uC7A1\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.",
                        "\uAD6C\uB984\uB9CE\uC74C", 20.1, 55.0, 30.0));

        DistrictRealtimeResponse response = hotplaceRealtimeService.getDistrictRealtime("\uC6A9\uC0B0\uAD6C");

        assertThat(response.getGuName()).isEqualTo("\uC6A9\uC0B0\uAD6C");
        assertThat(response.getSelectedAreaNm()).isEqualTo("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00");
        assertThat(response.getSummary().getWeatherStatus()).isEqualTo("\uB9D1\uC74C");
        assertThat(response.getSummary().getTemperature()).isEqualTo("21.3");
        assertThat(response.getSummary().getPm10()).isEqualTo("\uC88B\uC74C");
        assertThat(response.getSummary().getPrecipitationProbability()).isEqualTo("10%");
        assertThat(response.getHotplaces()).hasSize(2);
        assertThat(response.getHotplaces().get(0).getCategory()).isEqualTo("\uBB38\uD654\uC720\uC0B0");
        assertThat(response.getHotplaces().get(0).getAreaNm()).isEqualTo("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00");
        assertThat(response.getHotplaces().get(0).getCongestionLevel()).isEqualTo("\uC5EC\uC720");
        assertThat(response.getHotplaces().get(0).getCongestionMessage())
                .isEqualTo("\uC0AC\uB78C\uC774 \uBAB0\uB824\uC788\uC744 \uAC00\uB2A5\uC131\uC774 \uB0AE\uACE0 \uBD90\uBE54\uC740 \uAC70\uC758 \uB290\uAEF4\uC9C0\uC9C0 \uC54A\uC544\uC694.");
        assertThat(response.getHotplaces().get(1).getCategory()).isEqualTo("\uACF5\uC6D0");
        assertThat(response.getHotplaces().get(1).getAreaNm()).isEqualTo("\uC6A9\uC0B0\uACF5\uC6D0");
        assertThat(response.getHotplaces().get(1).getCongestionLevel()).isEqualTo("\uBCF4\uD1B5");
        assertThat(response.getHotplaces().get(1).getCongestionMessage())
                .isEqualTo("\uC77C\uBD80 \uAD6C\uAC04\uC774 \uD63C\uC7A1\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.");

        verify(hotPlaceRepository).findByGuName("\uC6A9\uC0B0\uAD6C");
    }

    @Test
    @DisplayName("같은 areaNm이 중복되면 외부 API는 한 번만 호출한다")
    void getDistrictRealtime_callsExternalApiOncePerAreaNm() {
        HotPlace first = hotPlace("\uC6A9\uC0B0\uAD6C", "\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00", Category.CULTURAL_HERITAGE);
        HotPlace second = hotPlace("\uC6A9\uC0B0\uAD6C", "\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00", Category.CULTURAL_HERITAGE);

        given(hotPlaceRepository.findByGuName("\uC6A9\uC0B0\uAD6C")).willReturn(List.of(first, second));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00"))
                .willReturn(realtimeData("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00", "\uC5EC\uC720",
                        "\uC815\uBCF4", "\uB9D1\uC74C", 21.3, 25.0, 10.0));

        hotplaceRealtimeService.getDistrictRealtime("\uC6A9\uC0B0\uAD6C");

        verify(seoulRealtimeClient, times(1)).getRealtimeDataByAreaNm("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00");
    }

    @Test
    @DisplayName("해당 구에 핫플레이스가 없으면 예외가 발생한다")
    void getDistrictRealtime_throwsWhenHotplaceNotFound() {
        given(hotPlaceRepository.findByGuName("\uC6A9\uC0B0\uAD6C")).willReturn(List.of());

        assertThatThrownBy(() -> hotplaceRealtimeService.getDistrictRealtime("\uC6A9\uC0B0\uAD6C"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.HOTPLACE_NOT_FOUND);
    }

    @Test
    @DisplayName("서울시 실시간 API 호출 실패 시 예외가 발생한다")
    void getDistrictRealtime_throwsWhenRealtimeApiFails() {
        HotPlace museum = hotPlace("\uC6A9\uC0B0\uAD6C", "\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00", Category.CULTURAL_HERITAGE);
        given(hotPlaceRepository.findByGuName("\uC6A9\uC0B0\uAD6C")).willReturn(List.of(museum));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00"))
                .willThrow(new CustomException(ErrorCode.SEOUL_REALTIME_API_CALL_FAILED));

        assertThatThrownBy(() -> hotplaceRealtimeService.getDistrictRealtime("\uC6A9\uC0B0\uAD6C"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SEOUL_REALTIME_API_CALL_FAILED);
    }

    @Test
    @DisplayName("외부 API 응답 일부 필드가 null이면 정보 없음으로 변환한다")
    void getDistrictRealtime_convertsNullFieldsToDefaultText() {
        HotPlace museum = hotPlace("\uC6A9\uC0B0\uAD6C", "\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00", Category.CULTURAL_HERITAGE);

        given(hotPlaceRepository.findByGuName("\uC6A9\uC0B0\uAD6C")).willReturn(List.of(museum));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00"))
                .willReturn(SeoulRealtimeData.builder()
                        .areaNm("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00")
                        .areaCongestLvl(null)
                        .areaCongestMsg(null)
                        .weatherStatus(null)
                        .temperature(null)
                        .pm10(null)
                        .rainChance(null)
                        .build());

        DistrictRealtimeResponse response = hotplaceRealtimeService.getDistrictRealtime("\uC6A9\uC0B0\uAD6C");

        assertThat(response.getSummary().getWeatherStatus()).isEqualTo("\uC815\uBCF4 \uC5C6\uC74C");
        assertThat(response.getSummary().getTemperature()).isEqualTo("\uC815\uBCF4 \uC5C6\uC74C");
        assertThat(response.getSummary().getPm10()).isEqualTo("\uC815\uBCF4 \uC5C6\uC74C");
        assertThat(response.getSummary().getPrecipitationProbability()).isEqualTo("\uC815\uBCF4 \uC5C6\uC74C");
        assertThat(response.getHotplaces().get(0).getCongestionLevel()).isEqualTo("\uC815\uBCF4 \uC5C6\uC74C");
        assertThat(response.getHotplaces().get(0).getCongestionMessage()).isEqualTo("\uC815\uBCF4 \uC5C6\uC74C");
    }

    private HotPlace hotPlace(String guName, String areaNm, Category category) {
        return HotPlace.builder()
                .id(1L)
                .guName(guName)
                .areaNm(areaNm)
                .category(category)
                .latitude(37.0)
                .longitude(127.0)
                .build();
    }

    private SeoulRealtimeData realtimeData(String areaNm,
                                           String congestionLevel,
                                           String congestionMessage,
                                           String weatherStatus,
                                           Double temperature,
                                           Double pm10,
                                           Double rainChance) {
        return SeoulRealtimeData.builder()
                .areaNm(areaNm)
                .areaCongestLvl(congestionLevel)
                .areaCongestMsg(congestionMessage)
                .weatherStatus(weatherStatus)
                .temperature(temperature)
                .pm10(pm10)
                .rainChance(rainChance)
                .build();
    }
}
