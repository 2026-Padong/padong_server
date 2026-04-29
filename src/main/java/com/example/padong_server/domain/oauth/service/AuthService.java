package com.example.padong_server.domain.oauth.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.oauth.dto.request.UserSignUpRequest;
import com.example.padong_server.domain.oauth.dto.response.SignUpResponse;
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

        JwtToken newToken = jwtProvider.createToken(userId);

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
        AdminDong adminDong = adminDongRepository.findById(request.adminDongId())
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_DONG_NOT_FOUND));

        User user = userRepository.findByKakaoId(request.kakaoId())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .kakaoId(request.kakaoId())
                                .nickname(request.nickname())
                                .picture(request.picture())
                                .email(request.email())
                                .adminDong(adminDong)
                                .build()
                ));

        user.updateProfile(request.nickname(), request.picture(), request.email(), adminDong);

        JwtToken token = jwtProvider.createToken(user.getId());
        refreshTokenService.save(
                user.getId(),
                token.getRefreshToken(),
                jwtProvider.getRefreshTokenExpireTime()
        );

        return SignUpResponse.of(user.getId(), adminDong.getAdminDongCode(), token);
    }
}
