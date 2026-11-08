package com.app.itibari.models.request;

import com.app.itibari.dataTypes.MobileUserType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MobileForgotPasswordRequest {

    private String phoneNumber;

    private MobileUserType mobileUserType;
}
