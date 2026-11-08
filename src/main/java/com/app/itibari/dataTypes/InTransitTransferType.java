package com.app.itibari.dataTypes;

/**
 * Defines the scope of an in-transit (or post-loading) transfer to another Delivery Agent.
 * ROUTE  – move every order on the source route to the new DA.
 * ORDER  – move only the specified orders; source route keeps the rest.
 */
public enum InTransitTransferType {
    ROUTE,
    ORDER
}

