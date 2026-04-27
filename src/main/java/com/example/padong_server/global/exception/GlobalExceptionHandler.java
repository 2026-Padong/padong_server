package com.example.padong_server.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .code(errorCode.getCode())
                        .message(exception.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }
}
