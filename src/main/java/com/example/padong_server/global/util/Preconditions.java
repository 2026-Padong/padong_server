package com.example.padong_server.global.util;

import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;

public final class Preconditions {

    private Preconditions() {}

    public static void validate(boolean condition, String message) {
        if (!condition) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, message);
        }
    }
}
