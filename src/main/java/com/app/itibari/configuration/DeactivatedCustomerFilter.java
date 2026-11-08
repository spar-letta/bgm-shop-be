package com.app.itibari.configuration;

import com.app.itibari.dataTypes.CustomerStatus;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.utils.exceptions.rest.ApiErrorCodes;
import com.app.itibari.utils.exceptions.rest.RestApiError;
import com.app.itibari.utils.exceptions.rest.RestApiHttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class DeactivatedCustomerFilter extends OncePerRequestFilter {

    private final CustomersRepository customersRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            boolean isCustomer = jwtAuth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority()));

            if (isCustomer) {
                String phoneNumber = jwtAuth.getToken().getSubject();
                Optional<Customers> optCustomer =
                        customersRepository.findByPhoneNumberAndDeletedIsFalse(phoneNumber);

                if (optCustomer.isPresent() && isDeactivated(optCustomer.get())) {
                    log.warn("Blocked API access for deactivated customer: phone={}", phoneNumber);
                    writeDeactivatedResponse(response);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isDeactivated(Customers customer) {
        return !customer.isActive() || CustomerStatus.De_Activated == customer.getStatus();
    }

    private void writeDeactivatedResponse(HttpServletResponse response) throws IOException {
        RestApiError error = new RestApiError(RestApiHttpStatus.FORBIDDEN);
        error.setApiCode(ApiErrorCodes.ACCOUNT_DEACTIVATED);
        error.setUserMessage("Your account has been deactivated. Please contact support.");
        error.setDeveloperMessage("Customer account is deactivated — request blocked at security filter.");

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
