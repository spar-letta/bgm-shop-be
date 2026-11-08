package com.app.itibari.configuration;

import com.app.itibari.dataTypes.MobileUserType;
import com.app.itibari.models.entities.Permission;
import com.app.itibari.models.entities.Role;
import com.app.itibari.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtToken {
    private final JwtDecoder jwtDecoder;

    private final JwtEncoder jwtEncoder;
    private final RoleRepository roleRepository;

    public JwtToken(JwtDecoder jwtDecoder, JwtEncoder jwtEncoder, RoleRepository roleRepository) {
        this.jwtDecoder = jwtDecoder;
        this.jwtEncoder = jwtEncoder;
        this.roleRepository = roleRepository;
    }

    public String generateJwtToken(Authentication authentication, List<String> roles) {
        Instant now = Instant.now();

        String onlyRoles = roles.stream()
                .filter(string -> string.startsWith("ROLE_"))
                .collect(Collectors.joining(","));

        String permissions = roles.stream()
                .filter(string -> !string.startsWith("ROLE_"))
                .collect(Collectors.joining(","));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .subject(authentication.getName())
                .issuedAt(now)
                .claim("roles", onlyRoles)
                .claim("permissions", permissions)
                .expiresAt(now.plusSeconds(24 * 60 * 60))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generateMobilePlatformJwtToken(MobileUserType mobileUserType, String phoneNumber) {

        return generateMobilePlatformJwtToken(mobileUserType, phoneNumber, null);
    }

    public String generateMobilePlatformJwtToken(MobileUserType mobileUserType, String phoneNumber, UUID sessionId) {

        String role;
        String permissions;

        if (mobileUserType == MobileUserType.Customer) {
            role = "ROLE_CUSTOMER";

            Optional<Role> roleOptional = roleRepository.findByRoleNameAndDeletedIsFalse(role);
            if (roleOptional.isPresent()) {
                Role roleObj = roleOptional.get();
                permissions = roleObj.getPermissions().stream()
                        .map(Permission::getName)
                        .collect(Collectors.joining(","));
            } else {
                permissions = "VIEW_PRODUCT";
            }
        } else if (mobileUserType == MobileUserType.DeliveryAgent) {
            role = "ROLE_DELIVERY_AGENT";

            Optional<Role> roleOptional = roleRepository.findByRoleNameAndDeletedIsFalse(role);
            if (roleOptional.isPresent()) {
                Role roleObj = roleOptional.get();
                permissions = roleObj.getPermissions().stream()
                        .map(Permission::getName)
                        .collect(Collectors.joining(","));
            } else {
                permissions = "VIEW_ORDER";
            }
        } else if (mobileUserType == MobileUserType.SalesAgent) {
            role = "ROLE_SALES_AGENT";
            Optional<Role> roleOptional = roleRepository.findByRoleNameAndDeletedIsFalse(role);
            if (roleOptional.isPresent()) {
                Role roleObj = roleOptional.get();
                permissions = roleObj.getPermissions().stream()
                        .map(Permission::getName)
                        .collect(Collectors.joining(","));
            } else {
                permissions = "VIEW_PRODUCT";
            }
        } else {
            role = "ROLE_MOBILE_USER";
            permissions = "VIEW_ORDER";
        }

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("self")
                .subject(phoneNumber)
                .issuedAt(Instant.now())
                .claim("roles", role)
                .claim("permissions", permissions)
                .expiresAt(Instant.now().plusSeconds(24 * 60 * 60 * 30));  // 30 days

        if (mobileUserType == MobileUserType.DeliveryAgent) {
            if (sessionId == null) throw new IllegalArgumentException("Delivery-agent session ID is required");
            claimsBuilder.claim("session_id", sessionId.toString());
        }

        JwtClaimsSet claims = claimsBuilder.build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
