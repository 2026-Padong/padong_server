package com.example.padong_server.domain.oauth.controller;

import com.example.padong_server.domain.oauth.dto.request.ReissueRequest;
import com.example.padong_server.domain.oauth.dto.request.UserSignUpRequest;
import com.example.padong_server.domain.oauth.dto.response.SignUpResponse;
import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.oauth.jwt.JwtToken;
import com.example.padong_server.domain.oauth.service.AuthService;
import com.example.padong_server.global.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/reissue")
    public ResponseEntity<ResponseDTO<JwtToken>> reissue(@RequestBody ReissueRequest request) {
        JwtToken token = authService.reissue(request.refreshToken());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "토큰 재발급 성공", token));
    }

    @PostMapping("/signup")
    public ResponseEntity<ResponseDTO<SignUpResponse>> signUp(
            @RequestBody UserSignUpRequest request
    ) {
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "회원가입 성공", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDTO<Void>> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getUserId());
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "로그아웃 성공"));
    }

    @GetMapping("/me")
    public ResponseEntity<ResponseDTO<Long>> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "내 정보 조회 성공", userDetails.getUserId()));
    }
}
