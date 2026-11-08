package com.app.itibari.services.supplierportal;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Normalizes optional single or multi geography filters for supplier-portal repository queries.
 * Empty lists mean no filter (all regions/locations). Non-empty lists use SQL {@code IN} clauses.
 */
public final class SupplierPortalGeographyFilter {

    /** Sentinel value when the corresponding filter is disabled; never matched because of the disabled flag. */
    private static final UUID PLACEHOLDER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final List<UUID> regionIds;
    private final List<UUID> locationIds;
    private final boolean regionFilterDisabled;
    private final boolean locationFilterDisabled;

    private SupplierPortalGeographyFilter(List<UUID> regionIds, List<UUID> locationIds) {
        this.regionFilterDisabled = regionIds.isEmpty();
        this.locationFilterDisabled = locationIds.isEmpty();
        this.regionIds = regionFilterDisabled ? List.of(PLACEHOLDER) : regionIds;
        this.locationIds = locationFilterDisabled ? List.of(PLACEHOLDER) : locationIds;
    }

    public static SupplierPortalGeographyFilter of(List<UUID> regionIds, List<UUID> locationIds) {
        return new SupplierPortalGeographyFilter(normalize(regionIds), normalize(locationIds));
    }

    public static SupplierPortalGeographyFilter of(UUID regionId, UUID locationId) {
        List<UUID> regions = regionId != null ? List.of(regionId) : List.of();
        List<UUID> locations = locationId != null ? List.of(locationId) : List.of();
        return of(regions, locations);
    }

    private static List<UUID> normalize(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    public List<UUID> getRegionIds() {
        return regionIds;
    }

    public List<UUID> getLocationIds() {
        return locationIds;
    }

    public boolean isRegionFilterDisabled() {
        return regionFilterDisabled;
    }

    public boolean isLocationFilterDisabled() {
        return locationFilterDisabled;
    }

    public boolean usesWardRankingBucket() {
        return !regionFilterDisabled || !locationFilterDisabled;
    }

    public List<UUID> regionIdsParam() {
        return regionIds;
    }

    public List<UUID> locationIdsParam() {
        return locationIds;
    }

    public boolean regionFilterDisabledParam() {
        return regionFilterDisabled;
    }

    public boolean locationFilterDisabledParam() {
        return locationFilterDisabled;
    }
}
