package com.app.itibari.models.request;

import com.app.itibari.dataTypes.IdentificationType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class SalesAgentsRequest {

    private String name;

    private String email;

    private String phoneNumber;

    private UUID organizationRegionId;

    private UUID salesAgentManagerPersonId;

    private IdentificationType identificationType;

    private String identificationNumber;

    private List<UUID> locationPublicIds;
}
