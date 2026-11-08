package com.app.itibari.models.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Builder
public class OrganizationCitiesRequest {

    private String code;

    private String name;

    private UUID organizationCountryId;
}
