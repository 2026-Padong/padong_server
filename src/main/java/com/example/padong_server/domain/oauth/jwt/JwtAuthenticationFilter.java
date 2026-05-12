package com.example.padong_server.domain.oauth.jwt;

import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.oauth.entity.Role;
import com.example.padong_server.domain.oauth.entity.User;
import com.example.padong_server.domain.oauth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);
        boolean mobilityDataRequest = "/mobility/data".equals(request.getRequestURI());

        if (mobilityDataRequest) {
            log.info(
                    "JwtAuthenticationFilter mobility data request: method={}, uri={}, origin={},"
                            + " authorizationPresent={}, bearerTokenPresent={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getHeader("Origin"),
                    request.getHeader("Authorization") != null,
                    token != null);
        }

        if (token != null && jwtProvider.validateToken(token)) {
            Long memberId = jwtProvider.getUserId(token);
            if (mobilityDataRequest) {
                log.info("JwtAuthenticationFilter valid token for mobility data request: userId={}", memberId);
            }

            User user = userRepository.findById(memberId)
                    .orElseThrow();

            if (!user.isRegistered() || (user.getRole() == Role.ADMIN && !user.isApproved())) {
                if (mobilityDataRequest) {
                    log.info(
                            "JwtAuthenticationFilter skipping authentication for mobility data request:"
                                    + " userId={}, registered={}, role={}, approved={}",
                            memberId,
                            user.isRegistered(),
                            user.getRole(),
                            user.isApproved());
                }
                filterChain.doFilter(request, response);
                return;
            }

            CustomUserDetails userDetails = new CustomUserDetails(user);

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            if (mobilityDataRequest) {
                log.info(
                        "JwtAuthenticationFilter authentication set for mobility data request:"
                                + " userId={}, authorities={}",
                        memberId,
                        userDetails.getAuthorities());
            }
        } else if (mobilityDataRequest) {
            log.info(
                    "JwtAuthenticationFilter continuing mobility data request without authentication:"
                            + " tokenPresent={}",
                    token != null);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");

        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        return null;
    }
}
