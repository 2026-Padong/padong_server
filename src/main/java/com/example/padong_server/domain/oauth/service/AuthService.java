package com.example.padong_server.domain.oauth.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongneLike.repository.DongneLikeRepository;
import com.example.padong_server.domain.storeLike.repository.StoreLikeRepository;
import com.example.padong_server.domain.oauth.dto.request.AdminUpgradeRequest;
import com.example.padong_server.domain.oauth.dto.request.UserSignUpRequest;
import com.example.padong_server.domain.oauth.dto.response.AdminUpgradeResponse;
import com.example.padong_server.domain.oauth.dto.response.SignUpResponse;
import com.example.padong_server.domain.oauth.dto.response.UserDetailResponse;
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
    private final DongneLikeRepository dongneLikeRepository;
    private final StoreLikeRepository storeLikeRepository;

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

        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_USER);
        }

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
    public void withdraw(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) {
            return;
        }
        dongneLikeRepository.deleteByUserId(userId);
        storeLikeRepository.deleteByUserId(userId);
        refreshTokenService.delete(userId);
        user.softDelete();
        // TODO: 본인이 등록한 가게(Store) 처리는 운영 정책 결정 후 추가 (현재는 그대로 둠)
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getMeDetail(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserDetailResponse.from(user);
    }

    @Transactional
    public AdminUpgradeResponse upgradeAdmin(Long userId, AdminUpgradeRequest request) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() == Role.ADMIN) {
            throw new CustomException(ErrorCode.INVALID_SIGNUP_REQUEST, "이미 ADMIN 사용자입니다.");
        }
        AdminDong adminDong = null;
        if (request.adminDongId() != null) {
            adminDong =
                    adminDongRepository
                            .findById(request.adminDongId())
                            .orElseThrow(
                                    () -> new CustomException(ErrorCode.ADMIN_DONG_NOT_FOUND));
        }
        user.upgradeToAdmin(request.businessLicenseImageUrl(), adminDong);
        refreshTokenService.delete(userId);
        return AdminUpgradeResponse.from(user);
    }

    @Transactional
    public UserDetailResponse updateMyAdminDong(Long userId, Long adminDongId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        AdminDong adminDong =
                adminDongRepository
                        .findById(adminDongId)
                        .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_DONG_NOT_FOUND));
        user.updateAdminDong(adminDong);
        return UserDetailResponse.from(user);
    }

    @Transactional
    public SignUpResponse signUp(UserSignUpRequest request) {
        validateSignUpRequest(request);

        AdminDong adminDong = null;
        if (request.role() == Role.USER) {
            adminDong = adminDongRepository.findById(request.adminDongId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_DONG_NOT_FOUND));
        }

        User user = userRepository.findByKakaoIdAndDeletedFalse(request.kakaoId())
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
