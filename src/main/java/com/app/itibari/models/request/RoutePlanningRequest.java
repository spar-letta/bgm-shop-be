package com.app.itibari.models.request;

import com.app.itibari.dataTypes.ActionType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RoutePlanningRequest {

    private UUID warehousePublicId;

    private UUID regionPublicId;

    private UUID locationPublicId;

    private UUID deliveryAgentPublicId;

    private UUID vehiclePublicId;

    private UUID routeWrapperPublicId;

    private List<UUID> salesOrdersPublicIds;

    private ActionType action;
}
