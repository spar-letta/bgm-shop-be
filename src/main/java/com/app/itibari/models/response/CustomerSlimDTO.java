package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CustomerSlimDTO {

    private UUID publicId;
    private String name;
    private String address;
    private String landmark;
    private String phoneNumber;
    private String latitude;
    private String longitude;
    private Boolean locationCoordinatesVerified;
}