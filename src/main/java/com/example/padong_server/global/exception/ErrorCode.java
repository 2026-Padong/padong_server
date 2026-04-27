package com.example.padong_server.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_GU_NAME(HttpStatus.BAD_REQUEST, "INVALID_GU_NAME", "존재하지 않는 자치구 이름입니다."),
    HOTPLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "HOTPLACE_NOT_FOUND", "해당 구에 등록된 핫플레이스가 없습니다."),
    POPULATION_NOT_FOUND(HttpStatus.NOT_FOUND, "POPULATION_NOT_FOUND", "해당 행정동의 인구 정보가 없습니다."),
    SEOUL_REALTIME_API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "SEOUL_REALTIME_API_CALL_FAILED", "서울시 실시간 도시데이터 API 호출에 실패했습니다."),
    SEOUL_REALTIME_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "SEOUL_REALTIME_DATA_NOT_FOUND", "해당 AREA_NM의 실시간 데이터를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
