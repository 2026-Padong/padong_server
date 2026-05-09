package com.example.padong_server.global.util;

import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;

public final class Preconditions {

    private Preconditions() {}

    public static void validate(boolean expression, ErrorCode errorCode) {
        if (!expression) {
            throw new CustomException(errorCode);
        }
    }
}
