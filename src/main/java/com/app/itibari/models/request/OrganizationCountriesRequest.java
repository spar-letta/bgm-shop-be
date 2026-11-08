package com.app.itibari.models.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class OrganizationCountriesRequest {

    private String code;

    private String name;

    private String address;

    private String customerCareSupervisor;
}
