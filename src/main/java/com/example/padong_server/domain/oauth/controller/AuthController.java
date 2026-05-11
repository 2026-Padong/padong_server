package com.example.padong_server.domain.oauth.controller;

import com.example.padong_server.domain.oauth.dto.request.AdminDongUpdateRequest;
import com.example.padong_server.domain.oauth.dto.request.AdminUpgradeRequest;
import com.example.padong_server.domain.oauth.dto.request.ReissueRequest;
import com.example.padong_server.domain.oauth.dto.request.UserSignUpRequest;
import com.example.padong_server.domain.oauth.dto.response.AdminUpgradeResponse;
import com.example.padong_server.domain.oauth.dto.response.SignUpResponse;
import com.example.padong_server.domain.oauth.dto.response.UserDetailResponse;
import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.oauth.jwt.JwtToken;
import com.example.padong_server.domain.oauth.service.AuthService;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(
        name = "Auth",
        description =
                """
                JWT 기반 인증 API.

                카카오 로그인은 Spring Security OAuth2 가 처리하며
                별도 REST endpoint 가 아닌 브라우저 redirect 흐름이다:

                  GET /oauth2/authorization/kakao?role=USER   (일반)
                  GET /oauth2/authorization/kakao?role=ADMIN  (사장님)

                성공 시 백엔드가 프론트(`app.oauth2.front-redirect`)로 redirect 하며
                query 에 다음 중 하나가 실린다:

                  - 신규: `?signupRequired=true&requestedRole=...&kakaoId=...&nickname=...&picture=...&email=...`
                  - 역할 불일치: `?roleMismatch=true&actualRole=...&requestedRole=...`
                  - ADMIN 승인 대기: `?pendingApproval=true&approved=false`
                  - 정상 로그인: `?accessToken=...&refreshToken=...&userId=...&nickname=...&role=...`
                """)
public class AuthController {

    private final AuthService authService;

    @PostMapping("/reissue")
    @Operation(
            summary = "토큰 재발급",
            description = "refreshToken 으로 새 accessToken/refreshToken 발급. JWT 인증 불필요.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "재발급 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ResponseDTO.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                                                        {
                                                          "statusCode": "200",
                                                          "message": "토큰 재발급 성공",
                                                          "data": {
                                                            "accessToken": "eyJhbGciOi...",
                                                            "refreshToken": "eyJhbGciOi..."
                                                          }
                                                        }
                                                        """))),
        @ApiResponse(responseCode = "401", description = "refreshToken 만료/위조"),
    })
    public ResponseEntity<ResponseDTO<JwtToken>> reissue(@RequestBody ReissueRequest request) {
        JwtToken token = authService.reissue(request.refreshToken());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "토큰 재발급 성공", token));
    }

    @PostMapping("/signup")
    @Operation(
            summary = "회원가입",
            description =
                    """
                    OAuth 콜백에서 받은 `signupRequired=true` 분기 후 호출.
                    kakaoId/nickname/picture/email + role(USER|ADMIN) + (USER 면) adminDongId 또는
                    (ADMIN 이면) businessLicenseImageUrl 를 함께 전송.
                    응답으로 첫 JWT 발급.
                    """)
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "회원가입 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ResponseDTO.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                                                        {
                                                          "statusCode": "200",
                                                          "message": "회원가입 성공",
                                                          "data": {
                                                            "userId": 5,
                                                            "role": "USER",
                                                            "approved": true,
                                                            "accessToken": "eyJhbGciOi...",
                                                            "refreshToken": "eyJhbGciOi..."
                                                          }
                                                        }
                                                        """))),
        @ApiResponse(responseCode = "400", description = "필수값 누락 / 존재하지 않는 행정동 등"),
    })
    public ResponseEntity<ResponseDTO<SignUpResponse>> signUp(
            @RequestBody UserSignUpRequest request) {
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "회원가입 성공", response));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "서버 측 refreshToken 폐기. 프론트는 클라이언트 토큰도 함께 제거.")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "401", description = "accessToken 누락/만료"),
    })
    public ResponseEntity<ResponseDTO<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getUserId());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "로그아웃 성공"));
    }

    @GetMapping("/me")
    @Operation(summary = "내 userId 조회", description = "현재 accessToken 의 userId 를 반환.")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ResponseDTO.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                                                        {
                                                          "statusCode": "200",
                                                          "message": "내 정보 조회 성공",
                                                          "data": 5
                                                        }
                                                        """))),
        @ApiResponse(responseCode = "401", description = "accessToken 누락/만료"),
    })
    public ResponseEntity<ResponseDTO<Long>> me(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "내 정보 조회 성공", userDetails.getUserId()));
    }

    @GetMapping("/me-detail")
    @Operation(
            summary = "프로필 상세 조회",
            description = "마이페이지 프로필 카드용. nickname/picture/email/role/approved/adminDong 반환.")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "프로필 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ResponseDTO.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                                                        {
                                                          "statusCode": "200",
                                                          "message": "프로필 조회 성공",
                                                          "data": {
                                                            "userId": 5,
                                                            "nickname": "지윤",
                                                            "picture": "https://k.kakaocdn.net/...",
                                                            "email": "jiyun@example.com",
                                                            "role": "USER",
                                                            "approved": true,
                                                            "adminDong": {
                                                              "adminDongCode": "1162069500",
                                                              "guName": "관악구",
                                                              "name": "신림동"
                                                            }
                                                          }
                                                        }
                                                        """))),
        @ApiResponse(responseCode = "401", description = "accessToken 누락/만료"),
        @ApiResponse(responseCode = "404", description = "사용자 미존재"),
    })
    public ResponseEntity<ResponseDTO<UserDetailResponse>> meDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "프로필 조회 성공",
                        authService.getMeDetail(userDetails.getUserId())));
    }

    @PutMapping("/me/admin-dong")
    @Operation(
            summary = "거주 행정동 변경",
            description = "마이페이지 '내 동네 설정' 용. 갱신된 프로필 상세 반환.")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "변경 성공 (응답은 me-detail 과 동일 형식)"),
        @ApiResponse(responseCode = "401", description = "accessToken 누락/만료"),
        @ApiResponse(responseCode = "404", description = "사용자 또는 행정동 미존재"),
    })
    public ResponseEntity<ResponseDTO<UserDetailResponse>> updateMyAdminDong(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @org.springframework.web.bind.annotation.RequestBody
                    @jakarta.validation.Valid AdminDongUpdateRequest request) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "거주 행정동 변경 성공",
                        authService.updateMyAdminDong(
                                userDetails.getUserId(), request.adminDongId())));
    }

    @PostMapping("/upgrade-admin")
    @Operation(
            summary = "USER → ADMIN 전환 신청",
            description =
                    "사장님 권한 신청. 신청 즉시 role 은 ADMIN 으로 바뀌지만 approved=false 라 로그인 진입은 막힘 (관리자 승인 필요). 신청 후 기존 RefreshToken 은 폐기되어 재로그인이 강제됨.")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "신청 완료"),
        @ApiResponse(responseCode = "400", description = "이미 ADMIN 이거나 필수값 누락"),
        @ApiResponse(responseCode = "401", description = "accessToken 누락/만료"),
        @ApiResponse(responseCode = "404", description = "사용자 또는 행정동 미존재"),
    })
    public ResponseEntity<ResponseDTO<AdminUpgradeResponse>> upgradeAdmin(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @org.springframework.web.bind.annotation.RequestBody
                    @jakarta.validation.Valid AdminUpgradeRequest request) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "사장님 전환 신청 완료",
                        authService.upgradeAdmin(userDetails.getUserId(), request)));
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "회원탈퇴",
            description =
                    """
                    User 는 soft delete (deleted=true, deletedAt=now, 닉네임·이메일 익명화).
                    좋아요(동네/가게) 는 hard delete. RefreshToken 폐기.
                    본인 등록 가게(Store) 는 일단 그대로 유지 — 운영 정책 결정 후 별도 처리.
                    """)
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "탈퇴 성공"),
        @ApiResponse(responseCode = "401", description = "accessToken 누락/만료"),
        @ApiResponse(responseCode = "404", description = "사용자 미존재"),
    })
    public ResponseEntity<ResponseDTO<Void>> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.withdraw(userDetails.getUserId());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "회원탈퇴 완료"));
    }
}
