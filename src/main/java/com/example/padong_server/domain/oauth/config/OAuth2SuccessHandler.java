package com.example.padong_server.domain.oauth.config;

import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.oauth.entity.Role;
import com.example.padong_server.domain.oauth.jwt.JwtProvider;
import com.example.padong_server.domain.oauth.jwt.JwtToken;
import com.example.padong_server.domain.oauth.repository.UserRepository;
import com.example.padong_server.domain.oauth.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        Map<String, Object> attributes = oAuth2User.getAttributes();

        Long kakaoId = Long.valueOf(attributes.get("id").toString());

        Map<String, Object> kakaoAccount = getAttributeMap(attributes, "kakao_account");

        Map<String, Object> profile = getAttributeMap(kakaoAccount, "profile");

        String nickname = (String) profile.get("nickname");
        String picture = (String) profile.get("profile_image_url");
        String email = (String) kakaoAccount.get("email");

        User user = userRepository.findByKakaoId(kakaoId).orElse(null);

        if (user == null || !user.isRegistered()) {
            String redirectUrl = "http://localhost:3000/oauth/callback"
                    + "?signupRequired=true"
                    + "&kakaoId=" + encode(String.valueOf(kakaoId))
                    + "&nickname=" + encode(nickname)
                    + "&picture=" + encode(picture)
                    + "&email=" + encode(email);

            response.sendRedirect(redirectUrl);
            return;
        }

        if (user.getRole() == Role.ADMIN && !user.isApproved()) {
            String redirectUrl = "http://localhost:3000/oauth/callback"
                    + "?pendingApproval=true"
                    + "&approved=false";

            response.sendRedirect(redirectUrl);
            return;
        }

        JwtToken token = jwtProvider.createToken(user);
        refreshTokenService.save(
                user.getId(),
                token.getRefreshToken(),
                jwtProvider.getRefreshTokenExpireTime()
        );

        String redirectUrl = "http://localhost:3000/oauth/callback"
                + "?accessToken=" + encode(token.getAccessToken())
                + "&refreshToken=" + encode(token.getRefreshToken());

        response.sendRedirect(redirectUrl);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAttributeMap(Map<String, Object> attributes, String key) {
        return (Map<String, Object>) attributes.get(key);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

}
