package com.example.padong_server.domain.hotplace.service;

import com.example.padong_server.domain.hotplace.dto.DistrictRealtimeResponse;
import com.example.padong_server.domain.hotplace.entity.Category;
import com.example.padong_server.domain.hotplace.entity.HotPlace;
import com.example.padong_server.domain.hotplace.repository.HotPlaceRepository;
import com.example.padong_server.domain.subway.service.SubwayTransferInfoService;
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

    @Mock
    private SubwayTransferInfoService subwayTransferInfoService;

    @InjectMocks
    private HotplaceRealtimeService hotplaceRealtimeService;

    @Test
    @DisplayName("guName으로 조회하면 요약 정보와 카드형 핫플레이스 목록을 반환한다")
    void getDistrictRealtime_returnsSummaryAndHotplaces() {
        HotPlace palace = hotPlace("종로구", "광화문·덕수궁", Category.CULTURAL_HERITAGE_COMPLEX);
        HotPlace market = hotPlace("종로구", "북촌한옥마을", Category.HANOK_VILLAGE);

        given(hotPlaceRepository.findByGuName("종로구")).willReturn(List.of(palace, market));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("광화문·덕수궁"))
                .willReturn(realtimeData(
                        "광화문·덕수궁",
                        "https://example.com/palace.jpg",
                        "서울시 종로구·중구 일대",
                        30000.0,
                        34000.0,
                        "여유",
                        "보행이 원활한 수준입니다.",
                        "Y",
                        28000.0,
                        31000.0,
                        "2026-05-04T14:00:00",
                        49.9,
                        50.1,
                        "맑음",
                        21.3,
                        22.0,
                        55.0,
                        "좋음",
                        18.0,
                        10.0,
                        11.2,
                        18.8,
                        23.8,
                        23.5,
                        14.1,
                        8.6,
                        0.0,
                        "원활",
                        13.0,
                        List.of("광화문역"),
                        List.of("5호선"),
                        List.of("세종문화회관", "광화문", "KT광화문지사"),
                        List.of("광화문역 2번 출구", "세종문화회관", "종로구청 앞", "정부서울청사", "덕수궁 대한문", "서울시청 서소문청사")));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("북촌한옥마을"))
                .willReturn(realtimeData(
                        "북촌한옥마을",
                        "https://example.com/village.jpg",
                        "서울시 종로구 북촌로 일대",
                        8000.0,
                        10000.0,
                        "보통",
                        "적정 수준입니다.",
                        "N",
                        null,
                        null,
                        null,
                        45.0,
                        55.0,
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
                        0.0,
                        "서행",
                        28.0,
                        List.of("안국역"),
                        List.of("3호선"),
                        List.of("안국역", "재동초등학교"),
                        List.of("안국역 1번 출구")));
        given(subwayTransferInfoService.findLinesByStationName("광화문역")).willReturn(List.of("5호선"));
        given(subwayTransferInfoService.findLinesByStationName("안국역")).willReturn(List.of("3호선"));

        DistrictRealtimeResponse response = hotplaceRealtimeService.getDistrictRealtime("종로구");

        assertThat(response.getGuName()).isEqualTo("종로구");
        assertThat(response.getSelectedAreaNm()).isEqualTo("광화문·덕수궁");
        assertThat(response.getSummary().getWeatherStatus()).isEqualTo("맑음");
        assertThat(response.getSummary().getTemperature()).isEqualTo("21.3");
        assertThat(response.getSummary().getSensibleTemperature()).isEqualTo("22.0");
        assertThat(response.getSummary().getHumidity()).isEqualTo("55%");
        assertThat(response.getSummary().getFineDustStatus()).isEqualTo("좋음");
        assertThat(response.getSummary().getFineDust()).isEqualTo("18.0");
        assertThat(response.getSummary().getPrecipitationProbability()).isEqualTo("10%");

        assertThat(response.getHotplaces()).hasSize(2);
        assertThat(response.getHotplaces().get(0).getAreaNm()).isEqualTo("광화문·덕수궁");
        assertThat(response.getHotplaces().get(0).getCategory()).isEqualTo("고궁·문화유산");
        assertThat(response.getHotplaces().get(0).getRoadAddr()).isEqualTo("서울시 종로구·중구 일대");
        assertThat(response.getHotplaces().get(0).getWeather().getWeatherStatus()).isEqualTo("맑음");
        assertThat(response.getHotplaces().get(0).getPopulation().getDisplay()).isEqualTo("약 32,000명");
        assertThat(response.getHotplaces().get(0).getPopulation().getForecast().getStatus()).isEqualTo("감소 예상");
        assertThat(response.getHotplaces().get(0).getAge().getDominantGroup()).isEqualTo("30대");
        assertThat(response.getHotplaces().get(0).getAge().getDominantRate()).isEqualTo("23.8%");
        assertThat(response.getHotplaces().get(0).getAge().getTop3()).hasSize(3);
        assertThat(response.getHotplaces().get(0).getAge().getTop3().get(1).getAgeGroup()).isEqualTo("40대");
        assertThat(response.getHotplaces().get(0).getGender().getFemaleRate()).isEqualTo("50.1%");
        assertThat(response.getHotplaces().get(0).getTransport().getSubway().getStationName()).isEqualTo("광화문역");
        assertThat(response.getHotplaces().get(0).getTransport().getSubway().getLines()).containsExactly("5호선");
        assertThat(response.getHotplaces().get(0).getTransport().getBus().getCount()).isEqualTo(3);
        assertThat(response.getHotplaces().get(0).getTransport().getBike().getCount()).isEqualTo(6);
        assertThat(response.getHotplaces().get(0).getRoadTraffic().getStatus()).isEqualTo("원활");
        assertThat(response.getHotplaces().get(0).getRoadTraffic().getSpeed()).isEqualTo("13km/h");

        verify(hotPlaceRepository).findByGuName("종로구");
    }

    @Test
    @DisplayName("같은 areaNm이 중복되면 외부 API는 한 번만 호출한다")
    void getDistrictRealtime_callsExternalApiOncePerAreaNm() {
        HotPlace first = hotPlace("종로구", "광화문·덕수궁", Category.CULTURAL_HERITAGE_COMPLEX);
        HotPlace second = hotPlace("종로구", "광화문·덕수궁", Category.CULTURAL_HERITAGE_COMPLEX);

        given(hotPlaceRepository.findByGuName("종로구")).willReturn(List.of(first, second));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("광화문·덕수궁"))
                .willReturn(realtimeData(
                        "광화문·덕수궁",
                        null,
                        null,
                        null,
                        null,
                        "여유",
                        null,
                        "N",
                        null,
                        null,
                        null,
                        null,
                        null,
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
                        null,
                        null,
                        List.of("광화문역"),
                        List.of("5호선"),
                        List.of(),
                        List.of()));
        given(subwayTransferInfoService.findLinesByStationName("광화문역")).willReturn(List.of("5호선"));

        hotplaceRealtimeService.getDistrictRealtime("종로구");

        verify(seoulRealtimeClient, times(1)).getRealtimeDataByAreaNm("광화문·덕수궁");
    }

    @Test
    @DisplayName("해당 구에 핫플레이스가 없으면 예외가 발생한다")
    void getDistrictRealtime_throwsWhenHotplaceNotFound() {
        given(hotPlaceRepository.findByGuName("종로구")).willReturn(List.of());

        assertThatThrownBy(() -> hotplaceRealtimeService.getDistrictRealtime("종로구"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.HOTPLACE_NOT_FOUND);
    }

    @Test
    @DisplayName("서울시 실시간 API 호출 실패 시 예외가 발생한다")
    void getDistrictRealtime_throwsWhenRealtimeApiFails() {
        HotPlace palace = hotPlace("종로구", "광화문·덕수궁", Category.CULTURAL_HERITAGE_COMPLEX);
        given(hotPlaceRepository.findByGuName("종로구")).willReturn(List.of(palace));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("광화문·덕수궁"))
                .willThrow(new CustomException(ErrorCode.SEOUL_REALTIME_API_CALL_FAILED));

        assertThatThrownBy(() -> hotplaceRealtimeService.getDistrictRealtime("종로구"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SEOUL_REALTIME_API_CALL_FAILED);
    }

    @Test
    @DisplayName("외부 API 응답 값이 null이면 정보 없음으로 변환한다")
    void getDistrictRealtime_convertsNullFieldsToDefaultText() {
        HotPlace palace = hotPlace("종로구", "광화문·덕수궁", null);

        given(hotPlaceRepository.findByGuName("종로구")).willReturn(List.of(palace));
        given(seoulRealtimeClient.getRealtimeDataByAreaNm("광화문·덕수궁"))
                .willReturn(SeoulRealtimeData.builder()
                        .areaNm("광화문·덕수궁")
                        .subwayStationNames(List.of())
                        .subwayLines(List.of())
                        .busStopNames(List.of())
                        .bikeStationNames(List.of())
                        .build());
        given(subwayTransferInfoService.findLinesByStationName("정보 없음")).willReturn(List.of());

        DistrictRealtimeResponse response = hotplaceRealtimeService.getDistrictRealtime("종로구");

        assertThat(response.getSummary().getWeatherStatus()).isEqualTo("정보 없음");
        assertThat(response.getSummary().getTemperature()).isEqualTo("정보 없음");
        assertThat(response.getSummary().getSensibleTemperature()).isEqualTo("정보 없음");
        assertThat(response.getSummary().getHumidity()).isEqualTo("정보 없음");
        assertThat(response.getSummary().getFineDustStatus()).isEqualTo("정보 없음");
        assertThat(response.getSummary().getFineDust()).isEqualTo("정보 없음");
        assertThat(response.getSummary().getPrecipitationProbability()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getCategory()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getRoadAddr()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getPopulation().getMin()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getPopulation().getDisplay()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getAge().getDominantGroup()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getAge().getDominantRate()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getGender().getMaleRate()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getTransport().getSubway().getStationName()).isEqualTo("정보 없음");
        assertThat(response.getHotplaces().get(0).getRoadTraffic().getSpeed()).isEqualTo("정보 없음");
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
                                           String thumbnail,
                                           String roadAddr,
                                           Double areaPpltnMin,
                                           Double areaPpltnMax,
                                           String congestionLevel,
                                           String congestionMessage,
                                           String fcstYn,
                                           Double fcstPpltnMin,
                                           Double fcstPpltnMax,
                                           String fcstTime,
                                           Double maleRate,
                                           Double femaleRate,
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
                                           Double ppltnRate70,
                                           String roadTrafficIdx,
                                           Double roadTrafficSpd,
                                           List<String> subwayStationNames,
                                           List<String> subwayLines,
                                           List<String> busStopNames,
                                           List<String> bikeStationNames) {
        return SeoulRealtimeData.builder()
                .areaNm(areaNm)
                .thumbnail(thumbnail)
                .roadAddr(roadAddr)
                .areaPpltnMin(areaPpltnMin)
                .areaPpltnMax(areaPpltnMax)
                .areaCongestLvl(congestionLevel)
                .areaCongestMsg(congestionMessage)
                .fcstYn(fcstYn)
                .fcstPpltnMin(fcstPpltnMin)
                .fcstPpltnMax(fcstPpltnMax)
                .fcstTime(fcstTime)
                .malePpltnRate(maleRate)
                .femalePpltnRate(femaleRate)
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
                .ppltnRate70(ppltnRate70)
                .roadTrafficIdx(roadTrafficIdx)
                .roadTrafficSpd(roadTrafficSpd)
                .subwayStationNames(subwayStationNames)
                .subwayLines(subwayLines)
                .busStopNames(busStopNames)
                .bikeStationNames(bikeStationNames)
                .build();
    }
}
