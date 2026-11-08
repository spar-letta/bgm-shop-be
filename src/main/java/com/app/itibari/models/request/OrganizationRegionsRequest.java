package com.app.itibari.models.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Builder
public class OrganizationRegionsRequest {

    private String code;

    private String name;

    private UUID organizationCityId;

    private String latitude;

    private String longitude;
}
