package com.example.padong_server.domain.oauth.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.oauth.dto.request.UserSignUpRequest;
import com.example.padong_server.domain.oauth.dto.response.SignUpResponse;
import com.example.padong_server.domain.oauth.entity.Role;
import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.oauth.jwt.JwtProvider;
import com.example.padong_server.domain.oauth.jwt.JwtToken;
import com.example.padong_server.domain.oauth.repository.UserRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final AdminDongRepository adminDongRepository;

    public JwtToken reissue(String refreshToken) {

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        if (!refreshTokenService.matches(userId, refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.isRegistered() || (user.getRole() == Role.ADMIN && !user.isApproved())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_USER);
        }

        JwtToken newToken = jwtProvider.createToken(user);

        refreshTokenService.save(
                userId,
                newToken.getRefreshToken(),
                jwtProvider.getRefreshTokenExpireTime()
        );

        return newToken;
    }

    public void logout(Long memberId) {
        refreshTokenService.delete(memberId);
    }

    @Transactional
    public SignUpResponse signUp(UserSignUpRequest request) {
        validateSignUpRequest(request);

        AdminDong adminDong = null;
        if (request.role() == Role.USER) {
            adminDong = adminDongRepository.findById(request.adminDongId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_DONG_NOT_FOUND));
        }

        User user = userRepository.findByKakaoId(request.kakaoId())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .kakaoId(request.kakaoId())
                                .nickname(request.nickname())
                                .picture(request.picture())
                                .email(request.email())
                                .role(request.role())
                                .build()
                ));

        user.updateProfile(request.nickname(), request.picture(), request.email(), adminDong);
        user.completeSignUp(request.role(), adminDong, request.businessLicenseImageUrl());

        JwtToken token = null;
        if (user.getRole() == Role.USER) {
            token = jwtProvider.createToken(user);
            refreshTokenService.save(
                    user.getId(),
                    token.getRefreshToken(),
                    jwtProvider.getRefreshTokenExpireTime()
            );
        }

        String adminDongCode = adminDong == null ? null : adminDong.getAdminDongCode();
        return SignUpResponse.of(
                user.getId(),
                user.getKakaoId(),
                user.getRole(),
                adminDongCode,
                user.isRegistered(),
                user.isApproved(),
                token
        );
    }

    private void validateSignUpRequest(UserSignUpRequest request) {
        if (request.role() == null) {
            throw new CustomException(ErrorCode.INVALID_SIGNUP_REQUEST, "role은 필수입니다.");
        }

        if (request.role() == Role.USER && request.adminDongId() == null) {
            throw new CustomException(ErrorCode.INVALID_SIGNUP_REQUEST, "USER 회원가입에는 adminDongId가 필요합니다.");
        }

        if (request.role() == Role.ADMIN && isBlank(request.businessLicenseImageUrl())) {
            throw new CustomException(ErrorCode.INVALID_SIGNUP_REQUEST, "ADMIN 회원가입에는 businessLicenseImageUrl이 필요합니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
