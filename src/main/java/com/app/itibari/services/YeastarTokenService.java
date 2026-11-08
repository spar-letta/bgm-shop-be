package com.app.itibari.services;

import com.app.itibari.configuration.YeastarConfiguration;
import com.app.itibari.models.request.YeastarTokenRequest;
import com.app.itibari.models.response.YeastarTokenResponse;
import com.app.itibari.utils.feign.YeastarApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class YeastarTokenService {

    private final YeastarApiClient yeastarApiClient;
    private final YeastarConfiguration yeastarConfiguration;

    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private final AtomicReference<String> cachedRefreshToken = new AtomicReference<>();
    private volatile long tokenExpiresAt = 0;

    public String getValidToken() {
        if (isTokenExpiringSoon()) {
            refreshOrFetchToken();
        }
        String token = cachedToken.get();
        if (token == null) {
            throw new IllegalStateException("Yeastar PBX token unavailable");
        }
        return token;
    }

    private boolean isTokenExpiringSoon() {
        long bufferMillis = (long) yeastarConfiguration.getTokenRefreshBufferSeconds() * 1000;
        return Instant.now().toEpochMilli() >= (tokenExpiresAt - bufferMillis);
    }

    private synchronized void refreshOrFetchToken() {
        // double-checked: another thread may have already refreshed
        if (!isTokenExpiringSoon()) return;

        String refreshToken = cachedRefreshToken.get();
        if (refreshToken != null) {
            try {
                YeastarTokenResponse response = yeastarApiClient.refreshToken(
                        YeastarTokenRequest.forRefresh(cachedToken.get(), refreshToken)
                );
                if (response.isSuccess()) {
                    storeToken(response);
                    log.debug("Yeastar token refreshed");
                    return;
                }
            } catch (Exception e) {
                log.warn("Yeastar token refresh failed, re-authenticating: {}", e.getMessage());
            }
        }
        fetchNewToken();
    }

    private void fetchNewToken() {
        YeastarTokenResponse response = yeastarApiClient.getToken(
                YeastarTokenRequest.forLogin(yeastarConfiguration.getAccessId(), yeastarConfiguration.getAccessKey())
        );
        if (!response.isSuccess()) {
            log.info("error {}  error code {} msg {}",response.errorDetail(),response.getErrcode(),response.getErrmsg());
            throw new IllegalStateException("Yeastar authentication failed: " + response.errorDetail());
        }
        storeToken(response);
        log.info("Yeastar PBX authenticated successfully");
    }

    private void storeToken(YeastarTokenResponse response) {
        cachedToken.set(response.getToken());
        cachedRefreshToken.set(response.getRefreshToken());
        // expiredTime is in seconds from now
        tokenExpiresAt = Instant.now().toEpochMilli() + (response.getExpiredTime() * 1000);
    }

    // Proactively refresh before expiry; runs every 25 minutes
    @Scheduled(fixedDelay = 1_500_000)
    public void scheduledTokenRefresh() {
        try {
            if (cachedToken.get() != null && isTokenExpiringSoon()) {
                refreshOrFetchToken();
            }
        } catch (Exception e) {
            log.error("Scheduled Yeastar token refresh failed: {}", e.getMessage());
        }
    }
}
