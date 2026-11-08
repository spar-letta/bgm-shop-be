package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmPasswordRequest {

    private String email;

    private String pin;

    private String password;

    private String confirmPassword;
}
