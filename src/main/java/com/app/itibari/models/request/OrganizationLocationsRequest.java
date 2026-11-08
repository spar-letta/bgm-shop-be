package com.app.itibari.models.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Builder
public class OrganizationLocationsRequest {

    private String code;

    private String name;

    private UUID organizationRegionId;

    private String latitude;

    private String longitude;
}
