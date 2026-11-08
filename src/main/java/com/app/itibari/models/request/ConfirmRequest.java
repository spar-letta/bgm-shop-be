package com.app.itibari.models.request;

import com.app.itibari.dataTypes.MobileUserType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmRequest {

    private String phoneNumber;

    private String otp;

    private MobileUserType mobileUserType;

    private String deviceId;

    private String deviceName;

    private String deviceModel;

    private String platform;
}
