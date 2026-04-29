package com.example.padong_server.domain.oauth.dto.response;

import com.example.padong_server.domain.oauth.jwt.JwtToken;

public record SignUpResponse(
        Long userId,
        String adminDongCode,
        String accessToken,
        String refreshToken
) {

    public static SignUpResponse of(Long userId, String adminDongCode, JwtToken token) {
        return new SignUpResponse(
                userId,
                adminDongCode,
                token.getAccessToken(),
                token.getRefreshToken()
        );
    }
}
