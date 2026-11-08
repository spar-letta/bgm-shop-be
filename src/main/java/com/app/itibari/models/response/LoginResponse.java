package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {

    private String token;

    private String username;

    private Boolean forcePasswordReset;

    public LoginResponse(String token, String username) {

        this.token = token;
        this.username = username;
    }

    public LoginResponse(String token, String username, Boolean forcePasswordReset) {

        this.token = token;
        this.username = username;
        this.forcePasswordReset = forcePasswordReset;
    }
}
