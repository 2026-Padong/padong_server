package com.example.padong_server.domain.oauth.dto.request;

public record UserSignUpRequest(
        Long kakaoId,
        String nickname,
        String picture,
        String email,
        Long adminDongId
) {
}
