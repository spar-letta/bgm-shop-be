package com.app.itibari.models.response;

import com.app.itibari.dataTypes.CustomerActiveStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CustomerLocationResponse {

    private UUID customerPublicId;

    private String customerName;

    private String latitude;

    private String longitude;

    private CustomerActiveStatus status;
}
