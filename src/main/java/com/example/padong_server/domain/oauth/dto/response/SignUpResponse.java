package com.example.padong_server.domain.oauth.dto.response;

import com.example.padong_server.domain.oauth.jwt.JwtToken;
import com.example.padong_server.domain.oauth.entity.Role;

public record SignUpResponse(
        Long userId,
        Long kakaoId,
        Role role,
        String adminDongCode,
        boolean registered,
        boolean approved,
        String accessToken,
        String refreshToken
) {

    public static SignUpResponse of(
            Long userId,
            Long kakaoId,
            Role role,
            String adminDongCode,
            boolean registered,
            boolean approved,
            JwtToken token
    ) {
        return new SignUpResponse(
                userId,
                kakaoId,
                role,
                adminDongCode,
                registered,
                approved,
                token == null ? null : token.getAccessToken(),
                token == null ? null : token.getRefreshToken()
        );
    }
}
