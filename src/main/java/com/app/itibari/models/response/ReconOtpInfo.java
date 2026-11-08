package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReconOtpInfo {
    private String customerName;
    private String customerPublicId;
    private String orderNumber;
    private String otpCode;
}
