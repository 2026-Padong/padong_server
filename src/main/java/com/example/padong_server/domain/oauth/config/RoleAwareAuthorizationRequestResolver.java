package com.example.padong_server.domain.oauth.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class RoleAwareAuthorizationRequestResolver
        implements OAuth2AuthorizationRequestResolver {

    public static final String REQUESTED_ROLE_SESSION_ATTR = "oauth.requestedRole";
    private static final String AUTHORIZATION_BASE = "/oauth2/authorization";

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public RoleAwareAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        this.delegate =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, AUTHORIZATION_BASE);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest req = delegate.resolve(request);
        if (req != null) {
            captureRequestedRole(request);
        }
        return req;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest req = delegate.resolve(request, clientRegistrationId);
        if (req != null) {
            captureRequestedRole(request);
        }
        return req;
    }

    private void captureRequestedRole(HttpServletRequest request) {
        String role = request.getParameter("role");
        if (role == null || role.isBlank()) {
            return;
        }
        String normalized = "ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER";
        request.getSession().setAttribute(REQUESTED_ROLE_SESSION_ATTR, normalized);
    }
}
