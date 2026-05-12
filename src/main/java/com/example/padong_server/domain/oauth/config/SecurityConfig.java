package com.example.padong_server.domain.oauth.config;

import com.example.padong_server.domain.oauth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // ─────────────────────────────── 비로그인 허용 ───────────────────────────────

    /** OAuth 인증·회원가입·토큰 흐름. */
    private static final String[] PUBLIC_AUTH = {
            "/oauth2/**",
            "/login/oauth2/**",
            "/auth/reissue",
            "/auth/signup",
            "/auth/admin/approve/**"
    };

    /** 동네/행정동 조회 (검색·트리·상세·추천). */
    private static final String[] PUBLIC_DONGNE = {
            "/dongne/admin-dongs",
            "/dongne/detail",
            "/dongne/recommendations/**",
            "/api/v1/dongne/recommendations/**",
            "/dongs/**"
    };

    /** 외부 공개 도메인 데이터 (뉴스·생활이동·실시간·길찾기). */
    private static final String[] PUBLIC_DOMAIN_DATA = {
            "/news/**",
            "/mobility/**",
            "/realtime/**",
            "/path/**"
    };

    /** Swagger / OpenAPI 문서. */
    private static final String[] PUBLIC_SWAGGER = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    /** 마스터 데이터 적재 endpoint (dev 편의 — 추후 ADMIN-only 후보). */
    private static final String[] PUBLIC_DATA_LOADERS_POST = {
            "/dongne/data",
            "/dongne/boundaries",
            "/population/data",
            "/population/density/data",
            "/mobility/data",
            "/mobility/safety/data",
            "/rent-price/data",
            "/pictures/data",
            "/pictures/mappings/admin-dong",
            "/realtime/data",
            "/store-statistics/data"
    };

    private static final String[] PUBLIC_DATA_LOADERS_GET = {
            "/subway/load",
            "/subway/transfers/load"
    };

    /** 게스트도 조회 가능한 사용자 측 도메인 endpoint. */
    private static final String[] PUBLIC_USER_GET = {
            "/stores",
            "/stores/categories",
            "/stores/random",
            "/stores/{storeId}",
            "/stores/{storeId}/images",
            "/menus",
            "/order-flows",
            "/api/recommendation-logs/latest"
    };

    // ─────────────────────────────── 빈 ───────────────────────────────

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final RoleAwareAuthorizationRequestResolver roleAwareAuthorizationRequestResolver;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // ── 비로그인 허용 ──
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_AUTH).permitAll()
                        .requestMatchers(PUBLIC_DONGNE).permitAll()
                        .requestMatchers(PUBLIC_DOMAIN_DATA).permitAll()
                        .requestMatchers(PUBLIC_SWAGGER).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_DATA_LOADERS_POST).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_DATA_LOADERS_GET).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_USER_GET).permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/recommendation-logs/**").permitAll()

                        // ── 가게 — 좁은 룰부터 (path variable 충돌 방지) ──
                        .requestMatchers("/stores/mine").hasRole("ADMIN")
                        .requestMatchers("/stores/likes", "/stores/likes/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/stores", "/stores/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/stores/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/stores/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/stores/**").hasRole("ADMIN")

                        // ── 메뉴 mutation — 사장 only ──
                        .requestMatchers(HttpMethod.POST, "/menus", "/menus/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/menus/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/menus/**").hasRole("ADMIN")

                        // ── 모임 mutation + 사장 전용 조회 ──
                        .requestMatchers("/order-flows/history", "/order-flows/history/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/order-flows/*/participants").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/order-flows", "/order-flows/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/order-flows/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/order-flows/**").hasRole("ADMIN")

                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ── 그 외 모두 인증 필요 ──
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint ->
                                endpoint.authorizationRequestResolver(roleAwareAuthorizationRequestResolver))
                        .successHandler(oAuth2SuccessHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
