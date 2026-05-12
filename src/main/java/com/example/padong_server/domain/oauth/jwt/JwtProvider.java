package com.example.padong_server.domain.oauth.jwt;

import com.example.padong_server.domain.oauth.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

    private final Key key;

    private final long accessTokenExpireTime = 1000L * 60 * 30;      // 30분

    @Getter
    private final long refreshTokenExpireTime = 1000L * 60 * 60 * 24 * 14; // 14일

    public JwtProvider(JwtProperties props) {
        byte[] keyBytes = Decoders.BASE64.decode(props.secret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public JwtToken createToken(User user) {
        String accessToken = createAccessToken(user);
        String refreshToken = createRefreshToken(user);

        return new JwtToken(accessToken, refreshToken);
    }

    public String createAccessToken(User user) {
        return createToken(user, accessTokenExpireTime);
    }

    public String createRefreshToken(User user) {
        return createToken(user, refreshTokenExpireTime);
    }

    private String createToken(User user, long expireTime) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireTime);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("userId", user.getId())
                .claim("kakaoId", user.getKakaoId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public Long getUserId(String token) {
        return Long.valueOf(
                Jwts.parser()
                        .verifyWith((SecretKey) key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject()
        );
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
