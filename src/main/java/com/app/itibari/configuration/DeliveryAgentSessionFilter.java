package com.app.itibari.configuration;

import com.app.itibari.services.DeliveryAgentLoginSessionService;
import com.app.itibari.utils.exceptions.rest.RestApiError;
import com.app.itibari.utils.exceptions.rest.RestApiHttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;


/**
 * This filter checks the validity of the delivery agent's login session for each request.
 * It extracts the session ID from the JWT token and validates it using the DeliveryAgentLoginSessionService.
 * If the session is invalid, expired, or inactive, it responds with an unauthorized error message.
 */
@RequiredArgsConstructor
public class DeliveryAgentSessionFilter extends OncePerRequestFilter {

    private final DeliveryAgentLoginSessionService sessionService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth && jwtAuth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DELIVERY_AGENT".equals(a.getAuthority()))) {
            try {
                String claim = jwtAuth.getToken().getClaimAsString("session_id");
                if (claim == null) throw new IllegalArgumentException("Missing session_id claim");
                sessionService.validate(UUID.fromString(claim), jwtAuth.getToken().getSubject());
            } catch (RuntimeException exception) {
                RestApiError error = new RestApiError(RestApiHttpStatus.UNAUTHORIZED);
                error.setUserMessage("Your login session has ended. Please log in again");
                error.setDeveloperMessage("Invalid, expired, or inactive delivery-agent session");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
