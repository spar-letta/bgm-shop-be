package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerOTPResponse {
    private String otp;
    private String message;
}
