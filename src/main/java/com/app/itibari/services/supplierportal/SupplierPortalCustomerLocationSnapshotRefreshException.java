package com.app.itibari.services.supplierportal;

/**
 * Thrown when a snapshot refresh must abort so the prior snapshot remains available to readers.
 */
public class SupplierPortalCustomerLocationSnapshotRefreshException extends RuntimeException {

    public SupplierPortalCustomerLocationSnapshotRefreshException(String message) {
        super(message);
    }

    public SupplierPortalCustomerLocationSnapshotRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}
