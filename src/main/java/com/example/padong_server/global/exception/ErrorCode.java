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

    // Payment
    GROUP_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP_ORDER_NOT_FOUND", "공구 정보를 찾을 수 없습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 정보를 찾을 수 없습니다."),
    INVALID_PAYMENT_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_REQUEST", "결제 요청값이 올바르지 않습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_STATUS", "현재 결제 상태에서는 요청한 작업을 수행할 수 없습니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_AMOUNT", "결제 금액이 일치하지 않습니다."),
    PAYMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PAYMENT_ACCESS_DENIED", "결제 정보에 접근할 수 없습니다."),
    INVALID_GROUP_ORDER_STATUS(HttpStatus.BAD_REQUEST, "INVALID_GROUP_ORDER_STATUS", "현재 공구 상태에서는 결제할 수 없습니다."),
    GROUP_ORDER_FULL(HttpStatus.BAD_REQUEST, "GROUP_ORDER_FULL", "공구 참여 가능 인원이 초과되었습니다."),
    INVALID_GROUP_ORDER_MENU(HttpStatus.BAD_REQUEST, "INVALID_GROUP_ORDER_MENU", "공구에 포함되지 않은 메뉴입니다."),
    SOLD_OUT_MENU(HttpStatus.BAD_REQUEST, "SOLD_OUT_MENU", "품절된 메뉴는 주문할 수 없습니다."),
    MIN_ORDER_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "MIN_ORDER_AMOUNT_NOT_MET", "최소 주문 금액을 만족하지 않습니다."),

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
