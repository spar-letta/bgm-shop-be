package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PackingRequest {

    private UUID agentId;

    private UUID vehicleId;

    private UUID routeId;
}
