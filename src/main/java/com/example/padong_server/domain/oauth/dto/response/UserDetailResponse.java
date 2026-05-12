package com.example.padong_server.domain.oauth.dto.response;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.oauth.entity.Role;
import com.example.padong_server.domain.oauth.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 프로필 상세")
public record UserDetailResponse(
        @Schema(description = "유저 PK", example = "5") Long userId,
        @Schema(description = "닉네임", example = "지윤") String nickname,
        @Schema(description = "프로필 사진 URL", example = "https://k.kakaocdn.net/...") String picture,
        @Schema(description = "이메일", example = "jiyun@example.com") String email,
        @Schema(description = "역할", example = "USER") Role role,
        @Schema(description = "ADMIN 승인 여부 (USER 는 항상 true)", example = "true") boolean approved,
        @Schema(description = "거주 행정동 (USER) / 영업 행정동 (ADMIN). null 가능") AdminDongRef adminDong) {

    @Schema(description = "행정동 요약")
    public record AdminDongRef(
            @Schema(description = "행정동 코드", example = "1162069500") String adminDongCode,
            @Schema(description = "자치구명", example = "관악구") String guName,
            @Schema(description = "행정동명", example = "신림동") String name) {

        public static AdminDongRef from(AdminDong dong) {
            if (dong == null) {
                return null;
            }
            return new AdminDongRef(
                    dong.getAdminDongCode(), dong.getDistrictName(), dong.getAdminDongName());
        }
    }

    public static UserDetailResponse from(User user) {
        return new UserDetailResponse(
                user.getId(),
                user.getNickname(),
                user.getPicture(),
                user.getEmail(),
                user.getRole(),
                user.isApproved(),
                AdminDongRef.from(user.getAdminDong()));
    }
}
