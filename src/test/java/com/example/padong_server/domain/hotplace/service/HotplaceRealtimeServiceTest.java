package com.example.padong_server.domain.hotplace.service;

import com.example.padong_server.domain.hotplace.dto.DistrictRealtimeResponse;
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
        HotPlace museum = hotPlace("용산구", "국립 중앙박물관");
        HotPlace park = hotPlace("용산구", "용산공원");

        given(hotPlaceRepository.findByGuName("용산구")).willReturn(List.of(museum, park));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("국립 중앙박물관"))
                .willReturn(realtimeData(
                        "국립 중앙박물관",
                        "https://example.com/museum.jpg",
                        "서울 용산구 서빙고로 137",
                        12000.0,
                        18000.0,
                        "여유",
                        "맑음",
                        21.3,
                        22.0,
                        55.0,
                        "좋음",
                        18.0,
                        10.0,
                        12.5,
                        31.2,
                        20.1,
                        15.0,
                        11.7,
                        9.5,
                        "원활",
                        42.5));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("용산공원"))
                .willReturn(realtimeData(
                        "용산공원",
                        "https://example.com/park.jpg",
                        "서울 용산구 용산동6가",
                        8000.0,
                        10000.0,
                        "보통",
                        "구름많음",
                        20.1,
                        19.4,
                        60.0,
                        "보통",
                        55.0,
                        30.0,
                        9.1,
                        18.0,
                        24.4,
                        21.0,
                        14.0,
                        13.5,
                        "서행",
                        28.0));

        DistrictRealtimeResponse response = hotplaceRealtimeService.getDistrictRealtime("용산구");

        assertThat(response.getGuName()).isEqualTo("용산구");
        assertThat(response.getSelectedAreaNm()).isEqualTo("국립 중앙박물관");
        assertThat(response.getSummary().getWeatherStatus()).isEqualTo("맑음");
        assertThat(response.getSummary().getTemperature()).isEqualTo("21.3");
        assertThat(response.getSummary().getSensibleTemperature()).isEqualTo("22.0");
        assertThat(response.getSummary().getHumidity()).isEqualTo("55%");
        assertThat(response.getSummary().getPm10Status()).isEqualTo("좋음");
        assertThat(response.getSummary().getPm10()).isEqualTo("18.0");
        assertThat(response.getSummary().getPrecipitationProbability()).isEqualTo("10%");
        assertThat(response.getHotplaces()).hasSize(2);
        assertThat(response.getHotplaces().get(0).getAreaNm()).isEqualTo("국립 중앙박물관");
        assertThat(response.getHotplaces().get(0).getThumbnail()).isEqualTo("https://example.com/museum.jpg");
        assertThat(response.getHotplaces().get(0).getRoadAddr()).isEqualTo("서울 용산구 서빙고로 137");
        assertThat(response.getHotplaces().get(0).getAreaPpltnMin()).isEqualTo("12000");
        assertThat(response.getHotplaces().get(0).getAreaPpltnMax()).isEqualTo("18000");
        assertThat(response.getHotplaces().get(0).getCongestionLevel()).isEqualTo("여유");
        assertThat(response.getHotplaces().get(0).getDominantAgeGroup()).isEqualTo("20대");
        assertThat(response.getHotplaces().get(0).getDominantAgeRate()).isEqualTo("31.2%");
        assertThat(response.getHotplaces().get(0).getRoadTrafficIdx()).isEqualTo("원활");
        assertThat(response.getHotplaces().get(0).getRoadTrafficSpd()).isEqualTo("42.5");
        assertThat(response.getHotplaces().get(1).getAreaNm()).isEqualTo("용산공원");
        assertThat(response.getHotplaces().get(1).getCongestionLevel()).isEqualTo("보통");
        assertThat(response.getHotplaces().get(1).getDominantAgeGroup()).isEqualTo("30대");
        assertThat(response.getHotplaces().get(1).getDominantAgeRate()).isEqualTo("24.4%");

        verify(hotPlaceRepository).findByGuName("용산구");
    }

    @Test
    @DisplayName("같은 areaNm이 중복되면 외부 API는 한 번만 호출한다")
    void getDistrictRealtime_callsExternalApiOncePerAreaNm() {
        HotPlace first = hotPlace("용산구", "국립 중앙박물관");
        HotPlace second = hotPlace("용산구", "국립 중앙박물관");

        given(hotPlaceRepository.findByGuName("용산구")).willReturn(List.of(first, second));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("국립 중앙박물관"))
                .willReturn(realtimeData(
                        "국립 중앙박물관",
                        null,
                        null,
                        null,
                        null,
                        "여유",
                        "맑음",
                        21.3,
                        22.0,
                        55.0,
                        "좋음",
                        18.0,
                        10.0,
                        10.0,
                        20.0,
                        30.0,
                        15.0,
                        12.0,
                        8.0,
                        null,
                        null));

        hotplaceRealtimeService.getDistrictRealtime("용산구");

        verify(seoulRealtimeClient, times(1)).getRealtimeDataByAreaNm("국립 중앙박물관");
    }

    @Test
    @DisplayName("해당 구에 핫플레이스가 없으면 예외가 발생한다")
    void getDistrictRealtime_throwsWhenHotplaceNotFound() {
        given(hotPlaceRepository.findByGuName("용산구")).willReturn(List.of());

        assertThatThrownBy(() -> hotplaceRealtimeService.getDistrictRealtime("용산구"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.HOTPLACE_NOT_FOUND);
    }

    @Test
    @DisplayName("서울시 실시간 API 호출 실패 시 예외가 발생한다")
    void getDistrictRealtime_throwsWhenRealtimeApiFails() {
        HotPlace museum = hotPlace("용산구", "국립 중앙박물관");
        given(hotPlaceRepository.findByGuName("용산구")).willReturn(List.of(museum));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("국립 중앙박물관"))
                .willThrow(new CustomException(ErrorCode.SEOUL_REALTIME_API_CALL_FAILED));

        assertThatThrownBy(() -> hotplaceRealtimeService.getDistrictRealtime("용산구"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SEOUL_REALTIME_API_CALL_FAILED);
    }

    @Test
    @DisplayName("외부 API 응답 값이 null이면 정보 없음으로 변환한다")
    void getDistrictRealtime_convertsNullFieldsToDefaultText() {
        HotPlace museum = hotPlace("용산구", "국립 중앙박물관");

        given(hotPlaceRepository.findByGuName("용산구")).willReturn(List.of(museum));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("국립 중앙박물관"))
                .willReturn(SeoulRealtimeData.builder()
                        .areaNm("국립 중앙박물관")
                        .areaCongestLvl(null)
                        .roadAddr(null)
                        .areaPpltnMin(null)
                        .areaPpltnMax(null)
                        .weatherStatus(null)
                        .temperature(null)
                        .sensibleTemperature(null)
                        .humidity(null)
                        .pm10Status(null)
                        .pm10(null)
                        .ppltnRate10(null)
                        .ppltnRate20(null)
                        .ppltnRate30(null)
                        .ppltnRate40(null)
                        .ppltnRate50(null)
                        .ppltnRate60(null)
                        .roadTrafficIdx(null)
                        .roadTrafficSpd(null)
                        .rainChance(null)
                        .build());

        DistrictRealtimeResponse response = hotplaceRealtimeService.getDistrictRealtime("용산구");

        assertThat(response.getSummary().getWeatherStatus()).isEqualTo("정보 없음");
        assertThat(response.getSummary().getTemperature()).isEqualTo("정보 없음");
        assertThat(response.getSummary().getSensibleTemperature()).isEqualTo("정보 없음");
        assertThat(response.getSummary().getHumidity()).isEqualTo("정보 없음");
        assertThat(response.getSummary().getPm10Status()).isEqualTo("정보 없음");
        assertThat(response.getSummary().getPm10()).isEqualTo("정보 없음");
        assertThat(response.getSummary().getPrecipitationProbability()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getCongestionLevel()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getRoadAddr()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getAreaPpltnMin()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getAreaPpltnMax()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getDominantAgeGroup()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getDominantAgeRate()).isEqualTo("정보 없음");
    }

    private HotPlace hotPlace(String guName, String areaNm) {
        return HotPlace.builder()
                .id(1L)
                .guName(guName)
                .areaNm(areaNm)
                .category(null)
                .latitude(37.0)
                .longitude(127.0)
                .build();
    }

    private SeoulRealtimeData realtimeData(String areaNm,
                                           String thumbnail,
                                           String roadAddr,
                                           Double areaPpltnMin,
                                           Double areaPpltnMax,
                                           String congestionLevel,
                                           String weatherStatus,
                                           Double temperature,
                                           Double sensibleTemperature,
                                           Double humidity,
                                           String pm10Status,
                                           Double pm10,
                                           Double rainChance,
                                           Double ppltnRate10,
                                           Double ppltnRate20,
                                           Double ppltnRate30,
                                           Double ppltnRate40,
                                           Double ppltnRate50,
                                           Double ppltnRate60,
                                           String roadTrafficIdx,
                                           Double roadTrafficSpd) {
        return SeoulRealtimeData.builder()
                .areaNm(areaNm)
                .thumbnail(thumbnail)
                .roadAddr(roadAddr)
                .areaPpltnMin(areaPpltnMin)
                .areaPpltnMax(areaPpltnMax)
                .areaCongestLvl(congestionLevel)
                .weatherStatus(weatherStatus)
                .temperature(temperature)
                .sensibleTemperature(sensibleTemperature)
                .humidity(humidity)
                .pm10Status(pm10Status)
                .pm10(pm10)
                .rainChance(rainChance)
                .ppltnRate10(ppltnRate10)
                .ppltnRate20(ppltnRate20)
                .ppltnRate30(ppltnRate30)
                .ppltnRate40(ppltnRate40)
                .ppltnRate50(ppltnRate50)
                .ppltnRate60(ppltnRate60)
                .roadTrafficIdx(roadTrafficIdx)
                .roadTrafficSpd(roadTrafficSpd)
                .build();
    }
}
