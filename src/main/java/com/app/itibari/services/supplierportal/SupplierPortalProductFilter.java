package com.app.itibari.services.supplierportal;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Normalizes optional single or multi product filters for supplier-portal repository queries.
 * Empty lists mean no filter (all supplier products). Non-empty lists use SQL {@code IN} clauses.
 */
public final class SupplierPortalProductFilter {

    /** Sentinel when the product filter is disabled; never matched because of the disabled flag. */
    private static final UUID PLACEHOLDER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final List<UUID> productIds;
    private final boolean productFilterDisabled;

    private SupplierPortalProductFilter(List<UUID> productIds) {
        this.productFilterDisabled = productIds.isEmpty();
        this.productIds = productFilterDisabled ? List.of(PLACEHOLDER) : productIds;
    }

    public static SupplierPortalProductFilter of(List<UUID> productIds) {
        return new SupplierPortalProductFilter(normalize(productIds));
    }

    public static SupplierPortalProductFilter of(UUID productId) {
        return of(productId != null ? List.of(productId) : List.of());
    }

    public static SupplierPortalProductFilter none() {
        return of(List.of());
    }

    private static List<UUID> normalize(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    public List<UUID> productIdsParam() {
        return productIds;
    }

    public boolean productFilterDisabledParam() {
        return productFilterDisabled;
    }
}
