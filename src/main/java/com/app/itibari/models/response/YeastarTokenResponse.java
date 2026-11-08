package com.app.itibari.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class YeastarTokenResponse {

    private String status;

    private String errmsg;

    private Integer errcode;

    @JsonProperty("access_token")
    private String token;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private long expiredTime;

    public boolean isSuccess() {
        return "Success".equalsIgnoreCase(status) || "0".equals(String.valueOf(errcode));
    }

    public String errorDetail() {
        if (errmsg != null) return errmsg;
        if (errcode != null) return "errcode=" + errcode;
        return status;
    }
}