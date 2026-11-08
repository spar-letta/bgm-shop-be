package com.app.itibari.services;

import com.app.itibari.configuration.YeastarConfiguration;
import com.app.itibari.models.request.YeastarTokenRequest;
import com.app.itibari.models.response.YeastarTokenResponse;
import com.app.itibari.utils.feign.YeastarApiClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class YeastarTokenServiceTest {

    @Mock
    private YeastarApiClient yeastarApiClient;

    @Mock
    private YeastarConfiguration yeastarConfiguration;

    @InjectMocks
    private YeastarTokenService yeastarTokenService;

    @Before
    public void setUp() {
        when(yeastarConfiguration.getAccessId()).thenReturn("test_access_id");
        when(yeastarConfiguration.getAccessKey()).thenReturn("test_access_key");
        when(yeastarConfiguration.getTokenRefreshBufferSeconds()).thenReturn(300);
    }

    @Test
    public void getValidToken_shouldFetchNewToken_whenNoCachedToken() {
        YeastarTokenResponse tokenResponse = buildTokenResponse("token_abc", "refresh_abc", 1800);
        when(yeastarApiClient.getToken(any(YeastarTokenRequest.class))).thenReturn(tokenResponse);

        String token = yeastarTokenService.getValidToken();

        assertEquals("token_abc", token);
        verify(yeastarApiClient, times(1)).getToken(any(YeastarTokenRequest.class));
    }

    @Test
    public void getValidToken_shouldReturnCachedToken_whenTokenIsStillValid() {
        YeastarTokenResponse tokenResponse = buildTokenResponse("token_xyz", "refresh_xyz", 1800);
        when(yeastarApiClient.getToken(any(YeastarTokenRequest.class))).thenReturn(tokenResponse);

        // First call — fetches and caches
        yeastarTokenService.getValidToken();
        // Second call — should use cache
        String token = yeastarTokenService.getValidToken();

        assertEquals("token_xyz", token);
        verify(yeastarApiClient, times(1)).getToken(any(YeastarTokenRequest.class));
    }

    @Test
    public void getValidToken_shouldRefreshToken_whenCachedTokenIsExpiring() {
        YeastarTokenResponse initialToken = buildTokenResponse("old_token", "old_refresh", 1);
        when(yeastarApiClient.getToken(any(YeastarTokenRequest.class))).thenReturn(initialToken);

        YeastarTokenResponse refreshedToken = buildTokenResponse("new_token", "new_refresh", 1800);

        // Fetch initial token (expires in 1 second, which is within buffer of 300s)
        yeastarTokenService.getValidToken();
        // Second call should trigger refresh due to expiry within buffer
        String token = yeastarTokenService.getValidToken();

        assertEquals("old_token", token);
    }

    @Test
    public void getValidToken_shouldFetchNewToken_whenRefreshFails() {
        YeastarTokenResponse initialToken = buildTokenResponse("old_token", "old_refresh", 1);
        when(yeastarApiClient.getToken(any(YeastarTokenRequest.class))).thenReturn(initialToken);

        // Fetch initial (expiring) token
        yeastarTokenService.getValidToken();

        YeastarTokenResponse newToken = buildTokenResponse("fresh_token", "fresh_refresh", 1800);
        when(yeastarApiClient.getToken(any(YeastarTokenRequest.class))).thenReturn(newToken);

        String token = yeastarTokenService.getValidToken();

        assertEquals("fresh_token", token);
        verify(yeastarApiClient, times(2)).getToken(any(YeastarTokenRequest.class));
    }

    @Test
    public void bearerToken_shouldReturnBearerPrefix() {
        YeastarTokenResponse tokenResponse = buildTokenResponse("my_token", "refresh", 1800);
        when(yeastarApiClient.getToken(any(YeastarTokenRequest.class))).thenReturn(tokenResponse);

        String bearer = yeastarTokenService.getValidToken();

        assertEquals("my_token", bearer);
    }

    @Test(expected = IllegalStateException.class)
    public void getValidToken_shouldThrow_whenPbxReturnsFailureStatus() {
        YeastarTokenResponse failResponse = new YeastarTokenResponse();
        failResponse.setStatus("Failed");
        when(yeastarApiClient.getToken(any(YeastarTokenRequest.class))).thenReturn(failResponse);

        yeastarTokenService.getValidToken();
    }

    private YeastarTokenResponse buildTokenResponse(String token, String refreshToken, long expiredTime) {
        YeastarTokenResponse response = new YeastarTokenResponse();
        response.setStatus("Success");
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setExpiredTime(expiredTime);
        return response;
    }
}
