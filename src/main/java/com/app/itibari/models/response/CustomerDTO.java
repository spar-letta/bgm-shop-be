package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CustomerDTO {

    private String name;

    private UUID customerPublicId;

    private String region;

    private String location;

    private String zone;

    private String landmark;

    private String latitude;

    private String longitude;

    private String phoneNumber;

}
