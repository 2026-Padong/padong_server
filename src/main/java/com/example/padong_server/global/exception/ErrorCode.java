package com.example.padong_server.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_GU_NAME(HttpStatus.BAD_REQUEST, "INVALID_GU_NAME", "\uC874\uC7AC\uD558\uC9C0 \uC54A\uB294 \uC790\uCE58\uAD6C \uC774\uB984\uC785\uB2C8\uB2E4."),
    HOTPLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "HOTPLACE_NOT_FOUND", "\uD574\uB2F9 \uAD6C\uC5D0 \uB4F1\uB85D\uB41C \uD56B\uD50C\uB808\uC774\uC2A4\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."),
    SEOUL_REALTIME_API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "SEOUL_REALTIME_API_CALL_FAILED", "\uC11C\uC6B8\uC2DC \uC2E4\uC2DC\uAC04 \uB3C4\uC2DC\uB370\uC774\uD130 API \uD638\uCD9C\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."),
    SEOUL_REALTIME_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "SEOUL_REALTIME_DATA_NOT_FOUND", "\uD574\uB2F9 AREA_NM\uC758 \uC2E4\uC2DC\uAC04 \uB370\uC774\uD130\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "\uC11C\uBC84 \uB0B4\uBD80 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
