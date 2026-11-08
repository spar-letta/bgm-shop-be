package com.app.itibari.services;

/**
 * Nightly refresh of the supplier portal customer location snapshot used by map markers and heat map.
 *
 * <p>Refresh is atomic: delete and insert run in one transaction. Any failure rolls back and leaves
 * the previous snapshot in place for API readers.</p>
 */
public interface SupplierPortalCustomerLocationsSnapshotService {

    /**
     * Rebuilds the snapshot from delivered orders with {@code creation_time} strictly before start of today.
     */
    void refreshSnapshot();
}
