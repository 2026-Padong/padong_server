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
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                .authorizeHttpRequests(auth -> auth
                        // ── CORS preflight ──
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── 공통 비로그인 허용 (OAuth, swagger, 조회 전용 endpoint) ──
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/auth/reissue",
                                "/auth/signup",
                                "/auth/admin/approve/**",
                                "/dongne/admin-dongs",
                                "/dongne/detail",
                                "/dongne/recommendations/**",
                                "/api/v1/dongne/recommendations/**",
                                "/dongs/**",
                                "/news/**",
                                "/mobility/**",
                                "/realtime/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/path/**"
                        ).permitAll()

                        // ── 마스터 데이터 적재 (dev 편의 — 추후 ADMIN-only 로 묶을 수 있음) ──
                        .requestMatchers(HttpMethod.POST,
                                "/dongne/data",
                                "/population/data",
                                "/population/density/data",
                                "/mobility/data",
                                "/mobility/safety/data",
                                "/rent-price/data",
                                "/pictures/data",
                                "/pictures/mappings/admin-dong",
                                "/hot-places/data",
                                "/store-statistics/data"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/subway/load",
                                "/subway/transfers/load"
                        ).permitAll()

                        // ── 가게 — 순서 중요: 좁은 패턴 → 넓은 패턴 ──
                        .requestMatchers("/stores/mine").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/stores/likes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/stores/likes/me").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/stores",
                                "/stores/categories",
                                "/stores/random",
                                "/stores/{storeId}",
                                "/stores/{storeId}/images").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/stores", "/stores/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/stores/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/stores/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/stores/**").hasRole("ADMIN")

                        // ── 기타 ──
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint ->
                                endpoint.authorizationRequestResolver(
                                        roleAwareAuthorizationRequestResolver))
                        .successHandler(oAuth2SuccessHandler)
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }
}
