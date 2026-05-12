package com.example.padong_server.domain.oauth.dto.request;

import com.example.padong_server.domain.oauth.entity.Role;

public record UserSignUpRequest(
        Long kakaoId,
        String nickname,
        String email,
        String picture,
        Role role,
        Long adminDongId) {
    // businessLicenseImageUrl 은 multipart 의 businessLicense file part 로부터 서버가 채움
}
