package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MobileResponse {

    private String action; // enter otp/ customer registration

    private String phoneNumber;

    private String confirmationCode;

    private String token;

    private String username;

    private Boolean forcePasswordReset;

    public MobileResponse(String action, String phoneNumber, String confirmationCode) {

        this.action = action;
        this.phoneNumber = phoneNumber;
        this.confirmationCode = confirmationCode;
    }

    public MobileResponse(String action, String phoneNumber, String confirmationCode, Boolean forcePasswordReset) {

        this.action = action;
        this.phoneNumber = phoneNumber;
        this.confirmationCode = confirmationCode;
        this.forcePasswordReset = forcePasswordReset;
    }

    public MobileResponse(String action, String phoneNumber, String token, String username, Boolean forcePasswordReset) {

        this.action = action;
        this.phoneNumber = phoneNumber;
        this.token = token;
        this.username = username;
        this.forcePasswordReset = forcePasswordReset;
    }
}
