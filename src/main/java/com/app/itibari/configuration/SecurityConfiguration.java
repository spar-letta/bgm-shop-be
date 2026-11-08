package com.app.itibari.configuration;

import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.services.DeliveryAgentLoginSessionService;
import com.app.itibari.utils.rsaKeys.RSAKeyProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by christopher oigo (oigoc60@gmail.com) on May 2025
 */


@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@Slf4j
public class SecurityConfiguration {
    public static final String WHITELIST = "/authentication/**";
    public static final String docs = "/api/docs/**";
    public static final String swagger = "/swagger-ui.html/**";
    public static final String swaggerUi = "/swagger-ui/**";
    public static final String apiDocs = "/v3/api-docs/**";
    public static final String scheduledTasks  = "/scheduledTasks/**";
    public static final String yeastarWebhook = "/yeastar/webhook";
    public static final String whatsAppCallBack = "/itibari-whatsapp";
    private final RSAKeyProperties rsaKeyProperties;
    private final CustomersRepository customersRepository;
    private final ObjectMapper objectMapper;
    private final DeliveryAgentLoginSessionService deliveryAgentLoginSessionService;

    public SecurityConfiguration(RSAKeyProperties rsaKeyProperties,
                                  CustomersRepository customersRepository,
                                  ObjectMapper objectMapper,
                                  DeliveryAgentLoginSessionService deliveryAgentLoginSessionService) {
        this.rsaKeyProperties = rsaKeyProperties;
        this.customersRepository = customersRepository;
        this.objectMapper = objectMapper;
        this.deliveryAgentLoginSessionService = deliveryAgentLoginSessionService;
    }

    @Bean
    public DeactivatedCustomerFilter deactivatedCustomerFilter() {
        return new DeactivatedCustomerFilter(customersRepository, objectMapper);
    }

    @Bean
    public DeliveryAgentSessionFilter deliveryAgentSessionFilter() {
        return new DeliveryAgentSessionFilter(deliveryAgentLoginSessionService, objectMapper);
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(WHITELIST, apiDocs, swagger, swaggerUi, docs, scheduledTasks, yeastarWebhook, whatsAppCallBack).permitAll()
                                .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2ResourceServer ->
                        oauth2ResourceServer.jwt(jwt ->
                                jwt.decoder(jwtDecoder())
                                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        ))
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterAfter(deactivatedCustomerFilter(), BearerTokenAuthenticationFilter.class);
        http.addFilterAfter(deliveryAgentSessionFilter(), DeactivatedCustomerFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            RSAPublicKey publicKey = rsaKeyProperties.getPublicKey();
            if (publicKey == null) {
                throw new IllegalStateException("RSA public key is not available");
            }
            return NimbusJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception e) {
            log.error("Failed to create JWT decoder", e);
            throw new RuntimeException("Could not create JWT decoder", e);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        try {
            RSAPublicKey publicKey = rsaKeyProperties.getPublicKey();
            RSAPrivateKey privateKey = rsaKeyProperties.getPrivateKey();
            
            if (publicKey == null || privateKey == null) {
                throw new IllegalStateException("RSA keys are not available");
            }
            
            JWK jwk = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(java.util.UUID.randomUUID().toString()) // Add a unique key ID
                    .build();
                    
            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
            return new NimbusJwtEncoder(jwkSource);
        } catch (Exception e) {
            log.error("Failed to create JWT encoder", e);
            throw new RuntimeException("Could not create JWT encoder", e);
        }
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        Converter<Jwt, Collection<GrantedAuthority>> converter = jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            // Handle roles claim
            try {
                Object rolesClaim = jwt.getClaim("roles");
                if (rolesClaim != null) {
                    String rolesStr = rolesClaim instanceof String ? (String) rolesClaim : "";
                    Arrays.stream(rolesStr.split(","))
                            .filter(role -> !role.isBlank())
                            .map(String::trim)
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                }
            } catch (Exception e) {
                log.warn("Failed to parse roles claim", e);
            }

            // Handle permissions claim
            try {
                Object permissionsClaim = jwt.getClaim("permissions");
                if (permissionsClaim != null) {
                    String permissionsStr = permissionsClaim instanceof String ? (String) permissionsClaim : "";
                    Arrays.stream(permissionsStr.split(","))
                            .filter(permission -> !permission.isBlank())
                            .map(String::trim)
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                }
            } catch (Exception e) {
                log.warn("Failed to parse permissions claim", e);
            }

            // Add default role if no authorities found (optional)
            if (authorities.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            log.debug("Converted JWT to authorities: {}", authorities);
            return authorities;
        };

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtAuthenticationConverter;
    }

}
