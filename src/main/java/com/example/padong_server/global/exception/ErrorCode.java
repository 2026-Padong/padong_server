package com.example.padong_server.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Oauth
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    INVALID_SIGNUP_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_SIGNUP_REQUEST", "회원가입 요청이 올바르지 않습니다."),
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_USER", "인증 정보가 없는 사용자입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 refresh token입니다."),

    // Common
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다."),

    // Dongne
    INVALID_GU_NAME(HttpStatus.BAD_REQUEST, "INVALID_GU_NAME", "존재하지 않는 자치구 이름입니다."),
    ADMIN_DONG_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_DONG_NOT_FOUND", "해당 행정동 정보를 찾을 수 없습니다."),

    // Hotplace / Store
    HOTPLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "HOTPLACE_NOT_FOUND", "해당 구에 등록된 핫플레이스가 없습니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "해당 가게 정보를 찾을 수 없습니다."),
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "MENU_NOT_FOUND", "해당 메뉴 정보를 찾을 수 없습니다."),
    ORDER_FLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_FLOW_NOT_FOUND", "해당 주문 흐름 정보를 찾을 수 없습니다."),
    INVALID_STORE_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_STORE_REQUEST", "가게 등록 요청값이 올바르지 않습니다."),
    INVALID_MENU_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_MENU_REQUEST", "메뉴 등록 요청값이 올바르지 않습니다."),
    INVALID_ORDER_FLOW_REQUEST(
            HttpStatus.BAD_REQUEST, "INVALID_ORDER_FLOW_REQUEST", "주문 흐름 요청값이 올바르지 않습니다."),
    INVALID_ORDER_FLOW_STATUS(
            HttpStatus.BAD_REQUEST, "INVALID_ORDER_FLOW_STATUS", "현재 주문 상태에서는 요청한 작업을 수행할 수 없습니다."),

    // Like
    INVALID_STORE_LIKE_REQUEST(
            HttpStatus.BAD_REQUEST, "INVALID_STORE_LIKE_REQUEST", "좋아요 요청값이 올바르지 않습니다."),
    INVALID_DONGNE_LIKE_REQUEST(
            HttpStatus.BAD_REQUEST, "INVALID_DONGNE_LIKE_REQUEST", "동네 좋아요 요청값이 올바르지 않습니다."),

    // Picture
    PICTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "PICTURE_NOT_FOUND", "해당 행정동에 이용 가능한 사진이 없습니다."),

    // Population
    POPULATION_NOT_FOUND(HttpStatus.NOT_FOUND, "POPULATION_NOT_FOUND", "해당 행정동의 인구 정보가 없습니다."),

    // External API
    ADDRESS_API_KEY_MISSING(
            HttpStatus.SERVICE_UNAVAILABLE, "ADDRESS_API_KEY_MISSING", "주소 변환 API 설정이 없습니다."),
    ADDRESS_API_CALL_FAILED(
            HttpStatus.BAD_GATEWAY, "ADDRESS_API_CALL_FAILED", "주소 변환 API 호출에 실패했습니다."),
    TOUR_API_KEY_MISSING(
            HttpStatus.SERVICE_UNAVAILABLE, "TOUR_API_KEY_MISSING", "관광 정보 API 설정이 없습니다."),
    TOUR_API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "TOUR_API_CALL_FAILED", "관광 정보 API 호출에 실패했습니다."),
    SEOUL_REALTIME_API_CALL_FAILED(
            HttpStatus.BAD_GATEWAY,
            "SEOUL_REALTIME_API_CALL_FAILED",
            "서울시 실시간 데이터 API 호출에 실패했습니다."),
    SEOUL_REALTIME_DATA_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SEOUL_REALTIME_DATA_NOT_FOUND",
            "해당 AREA_NM의 실시간 데이터를 찾을 수 없습니다."),
    ODSAY_API_KEY_MISSING(
            HttpStatus.SERVICE_UNAVAILABLE,
            "ODSAY_API_KEY_MISSING",
            "대중교통 길찾기 API 설정이 없습니다."),
    ODSAY_API_CALL_FAILED(
            HttpStatus.BAD_GATEWAY,
            "ODSAY_API_CALL_FAILED",
            "대중교통 길찾기 API 호출에 실패했습니다."),
    ODSAY_API_INVALID_PARAM(
            HttpStatus.BAD_REQUEST,
            "ODSAY_API_INVALID_PARAM",
            "대중교통 길찾기 요청 값이 올바르지 않습니다."),
    ODSAY_NO_DEPARTURE_STATION(
            HttpStatus.NOT_FOUND,
            "ODSAY_NO_DEPARTURE_STATION",
            "출발 행정동 근처에 대중교통 정류장이 없습니다."),
    ODSAY_NO_ARRIVAL_STATION(
            HttpStatus.NOT_FOUND,
            "ODSAY_NO_ARRIVAL_STATION",
            "도착 행정동 근처에 대중교통 정류장이 없습니다."),
    ODSAY_NO_STATION(
            HttpStatus.NOT_FOUND,
            "ODSAY_NO_STATION",
            "출/도착 행정동 근처에 대중교통 정류장이 없습니다."),
    ODSAY_OUT_OF_SERVICE_AREA(
            HttpStatus.NOT_FOUND,
            "ODSAY_OUT_OF_SERVICE_AREA",
            "대중교통 길찾기 서비스 지역이 아닙니다."),
    ODSAY_TOO_CLOSE(
            HttpStatus.BAD_REQUEST,
            "ODSAY_TOO_CLOSE",
            "출발 행정동과 도착 행정동이 700m 이내입니다."),
    ODSAY_NO_RESULT(
            HttpStatus.NOT_FOUND,
            "ODSAY_NO_RESULT",
            "대중교통 길찾기 결과가 없습니다."),
    TRANSIT_PATH_SAME_DONG(
            HttpStatus.BAD_REQUEST,
            "TRANSIT_PATH_SAME_DONG",
            "출발 행정동과 도착 행정동이 같습니다."),
    SK_PEDESTRIAN_API_KEY_MISSING(
            HttpStatus.SERVICE_UNAVAILABLE,
            "SK_PEDESTRIAN_API_KEY_MISSING",
            "보행자 경로 API 설정이 없습니다."),
    SK_PEDESTRIAN_API_CALL_FAILED(
            HttpStatus.BAD_GATEWAY,
            "SK_PEDESTRIAN_API_CALL_FAILED",
            "보행자 경로 API 호출에 실패했습니다."),
    SK_PEDESTRIAN_NO_RESULT(
            HttpStatus.NOT_FOUND,
            "SK_PEDESTRIAN_NO_RESULT",
            "보행자 경로 결과가 없습니다."),

    // RentPrice
    RENT_PRICE_TRADE_TYPE_REQUIRED(
            HttpStatus.BAD_REQUEST, "RENT_PRICE_TRADE_TYPE_REQUIRED", "거래 형태가 비어 있을 수 없습니다."),
    RESIDENCE_BUILDING_TYPE_REQUIRED(
            HttpStatus.BAD_REQUEST, "RESIDENCE_BUILDING_TYPE_REQUIRED", "집 형태가 비어 있을 수 없습니다."),
    INVALID_RENT_PRICE_TRADE_TYPE(
            HttpStatus.BAD_REQUEST, "INVALID_RENT_PRICE_TRADE_TYPE", "지원하지 않는 거래 형태입니다."),
    INVALID_RESIDENCE_BUILDING_TYPE(
            HttpStatus.BAD_REQUEST, "INVALID_RESIDENCE_BUILDING_TYPE", "지원하지 않는 집 형태입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
