package com.app.itibari.dataTypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Export view selector for GET /api/v1/supplier-portal/product-analysis/export.
 * Each value maps to a specific V2 data method and tabular layout.
 */
public enum SupplierPortalProductAnalysisView {

    /** Single-product ward performance (requires productId). */
    WARD_PERFORMANCE("Ward Performance"),

    /** All-products ward performance (no productId). */
    ALL_PRODUCTS_WARD_PERFORMANCE("All Products Ward Performance"),

    /** All-products product ranking by COGS (no productId). */
    ALL_PRODUCTS_PRODUCT_RANKING_COGS("All Products Product Ranking"),

    /** Single-product district ranking by settled tonnes. */
    DISTRICT_RANKING("District Ranking"),

    /** All-products district ranking by settled tonnes. */
    ALL_PRODUCTS_DISTRICT_RANKING("All Products District Ranking"),

    /** Single-product district COGS breakdown. */
    DISTRICT_COGS("District COGS"),

    /** All-products district COGS breakdown. */
    ALL_PRODUCTS_DISTRICT_COGS("All Products District COGS");

    public final String displayName;

    SupplierPortalProductAnalysisView(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return this.displayName;
    }

    @JsonCreator
    public static SupplierPortalProductAnalysisView forValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("SupplierPortalProductAnalysisView must not be null");
        }
        for (SupplierPortalProductAnalysisView v : values()) {
            if (v.name().equalsIgnoreCase(value) || v.displayName.equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown SupplierPortalProductAnalysisView: " + value);
    }
}
