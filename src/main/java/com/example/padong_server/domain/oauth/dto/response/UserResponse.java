package com.example.padong_server.domain.oauth.dto.response;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.oauth.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private Long kakaoId;
    private String nickname;
    private String picture;
    private String email;
    private Long adminDongId;
    private String adminDongCode;
    private String cityName;
    private String districtName;
    private String adminDongName;

    public static UserResponse from(User user) {
        AdminDong adminDong = user.getAdminDong();
        return UserResponse.builder()
                .id(user.getId())
                .kakaoId(user.getKakaoId())
                .nickname(user.getNickname())
                .picture(user.getPicture())
                .email(user.getEmail())
                .adminDongId(adminDong.getId())
                .adminDongCode(adminDong.getAdminDongCode())
                .cityName(adminDong.getCityName())
                .districtName(adminDong.getDistrictName())
                .adminDongName(adminDong.getAdminDongName())
                .build();
    }
}
