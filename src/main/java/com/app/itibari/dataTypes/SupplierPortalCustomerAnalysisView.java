package com.app.itibari.dataTypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Export view selector for GET /api/v1/supplier-portal/customer-analysis/export.
 * Each value maps to a specific V2 data method and tabular layout.
 */
public enum SupplierPortalCustomerAnalysisView {

    /** Paged customer rows with orders, COGS, tonnes, last-order date. */
    CUSTOMERS("Customers"),

    /** Top wards by distinct active customer count. */
    DISTRICT_RANKING("District Ranking"),

    /** Top wards by portal COGS. */
    DISTRICT_COGS("District COGS");

    public final String displayName;

    SupplierPortalCustomerAnalysisView(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return this.displayName;
    }

    @JsonCreator
    public static SupplierPortalCustomerAnalysisView forValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("SupplierPortalCustomerAnalysisView must not be null");
        }
        for (SupplierPortalCustomerAnalysisView v : values()) {
            if (v.name().equalsIgnoreCase(value) || v.displayName.equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown SupplierPortalCustomerAnalysisView: " + value);
    }
}
