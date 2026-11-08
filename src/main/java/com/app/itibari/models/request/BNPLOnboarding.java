package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BNPLOnboarding {
    private UUID customerPublicId;
    private String customerID;
    private String tinNumber;
    private String businessLicenseNumber;
    private String guarantorName;
    private String guarantorPhoneNumber;
}
