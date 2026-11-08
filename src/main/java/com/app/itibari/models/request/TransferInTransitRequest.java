package com.app.itibari.models.request;

import com.app.itibari.dataTypes.InTransitTransferType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Request payload for transferring a route or individual orders to another
 * Delivery Agent while the original route is in a Loading, Loaded, ReadyToGo, or IN_TRANSIT state
 * (e.g. in the event of a vehicle breakdown).
 */
@Getter
@Setter
public class TransferInTransitRequest {

    /** The public-id of the source route planning record. */
    @NotNull(message = "Source route public id is required")
    private UUID sourceRoutePublicId;

    /** The Delivery Agent who will take over. */
    @NotNull(message = "Target delivery agent public id is required")
    private UUID targetDeliveryAgentPublicId;

    /**
     * Required when the target agent has no active route.
     * Both transfer types use the active target route's vehicle when one exists.
     */
    private UUID targetVehiclePublicId;

    /** Optional reason / note (e.g. "vehicle breakdown on Mwai Kibaki Road"). */
    private String reason;

    /**
     * ROUTE – transfer every order on the source route.
     * ORDER – transfer only the orders listed in {@link #orderPublicIds}.
     */
    @NotNull(message = "Transfer type is required (ROUTE or ORDER)")
    private InTransitTransferType transferType;

    /**
     * Required only when transferType == ORDER.
     * Public-ids of the sales orders that should be moved to the new DA.
     */
    private List<UUID> orderPublicIds = new ArrayList<>();

    private UUID reasonCodePublicId;

}
