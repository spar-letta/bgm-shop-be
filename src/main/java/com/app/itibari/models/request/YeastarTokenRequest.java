package com.app.itibari.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YeastarTokenRequest {

    @JsonProperty("username")
    private String accessId;

    @JsonProperty("password")
    private String accessSecret;

    @JsonProperty("grant_type")
    private String grantType;

    @JsonProperty("ys_id")
    private String yesId;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    public static YeastarTokenRequest forLogin(String accessId, String accessSecret) {
        YeastarTokenRequest r = new YeastarTokenRequest();
        r.accessId = accessId;
        r.accessSecret = accessSecret;
        r.grantType = "access_token";
        return r;
    }

    public static YeastarTokenRequest forRefresh(String accessToken, String refreshToken) {
        YeastarTokenRequest r = new YeastarTokenRequest();
        r.accessToken = accessToken;
        r.refreshToken = refreshToken;
        r.grantType = "refresh_token";
        return r;
    }
}
