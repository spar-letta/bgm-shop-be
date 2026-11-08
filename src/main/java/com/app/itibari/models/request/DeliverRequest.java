package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class DeliverRequest {

    private UUID routePublicId;

    private List<UUID> orderPublicIds;
}
