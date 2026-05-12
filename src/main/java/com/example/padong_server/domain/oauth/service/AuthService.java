package com.example.padong_server.domain.oauth.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongneLike.repository.DongneLikeRepository;
import com.example.padong_server.domain.storeLike.repository.StoreLikeRepository;
import com.example.padong_server.domain.upload.service.S3FileUploader;
import org.springframework.web.multipart.MultipartFile;
import com.example.padong_server.domain.oauth.dto.request.AdminUpgradeRequest;
import com.example.padong_server.domain.oauth.dto.request.UpdateProfileRequest;
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
    private final S3FileUploader s3FileUploader;

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
    public void approveAdmin(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        // role 무관: USER 였으면 ADMIN 으로 승격까지 한 번에 처리. 이미 ADMIN 이면 approve 만.
        user.promoteToAdmin();
        refreshTokenService.delete(userId); // 재로그인 시 새 토큰에 role/approved 반영
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
    public AdminUpgradeResponse upgradeAdmin(
            Long userId, AdminUpgradeRequest request, MultipartFile businessLicense) {
        if (businessLicense == null || businessLicense.isEmpty()) {
            throw new CustomException(
                    ErrorCode.INVALID_SIGNUP_REQUEST, "사업자등록증 파일은 필수입니다.");
        }
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
        String url = s3FileUploader.uploadBusinessLicense(businessLicense, userId);
        user.upgradeToAdmin(url, adminDong);
        refreshTokenService.delete(userId);
        return AdminUpgradeResponse.from(user);
    }

    @Transactional
    public UserDetailResponse updateMyProfile(
            Long userId, UpdateProfileRequest request, MultipartFile picture) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateNickname(request.nickname().trim());

        if (picture != null && !picture.isEmpty()) {
            String previous = user.getPicture();
            String newUrl = s3FileUploader.uploadProfilePicture(picture, userId);
            user.updatePicture(newUrl);
            s3FileUploader.deleteIfOwned(previous);
        }

        return UserDetailResponse.from(user);
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
    public SignUpResponse signUp(UserSignUpRequest request, MultipartFile businessLicense) {
        validateSignUpRequest(request, businessLicense);

        AdminDong adminDong = null;
        if (request.role() == Role.USER) {
            adminDong = adminDongRepository.findById(request.adminDongId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_DONG_NOT_FOUND));
        }

        User user = userRepository.findByKakaoId(request.kakaoId())
                .map(existing -> {
                    if (existing.isDeleted()) {
                        existing.reactivate();
                    }
                    return existing;
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .kakaoId(request.kakaoId())
                                .nickname(request.nickname())
                                .picture(request.picture())
                                .email(request.email())
                                .role(request.role())
                                .build()
                ));

        // ADMIN 이면 user 가 영속화된 후 (id 확보) S3 업로드 — 실패 시 tx 롤백으로 user 도 정리됨
        String businessLicenseUrl = null;
        if (request.role() == Role.ADMIN) {
            businessLicenseUrl = s3FileUploader.uploadBusinessLicense(businessLicense, user.getId());
        }

        user.updateProfile(request.nickname(), request.picture(), request.email(), adminDong);
        user.completeSignUp(request.role(), adminDong, businessLicenseUrl);

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

    private void validateSignUpRequest(UserSignUpRequest request, MultipartFile businessLicense) {
        if (request.role() == null) {
            throw new CustomException(ErrorCode.INVALID_SIGNUP_REQUEST, "role은 필수입니다.");
        }

        if (request.role() == Role.USER && request.adminDongId() == null) {
            throw new CustomException(ErrorCode.INVALID_SIGNUP_REQUEST, "USER 회원가입에는 adminDongId가 필요합니다.");
        }

        if (request.role() == Role.ADMIN
                && (businessLicense == null || businessLicense.isEmpty())) {
            throw new CustomException(
                    ErrorCode.INVALID_SIGNUP_REQUEST, "ADMIN 회원가입에는 사업자등록증 파일이 필요합니다.");
        }
    }
}
