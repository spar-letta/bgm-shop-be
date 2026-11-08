package com.app.itibari.services;

import com.app.itibari.dataTypes.CustomerLocationMarkerCategoryV2;
import com.app.itibari.models.report.CustomerLocationDetailV2DTO;
import com.app.itibari.models.report.CustomerLocationsHeatMapV2DTO;
import com.app.itibari.models.report.CustomerLocationsMarkersV2DTO;

import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Supplier Portal Customer Locations V2 — map markers and marker-click detail.
 *
 * <p>Phase 2: no dedicated {@code /customer-locations/filters} endpoint. Filter dropdowns reuse the
 * same sources as Dashboard V2, Product Analysis V2, and Customer Analysis V2:</p>
 * <ul>
 *   <li>Regions — {@code GET /authentication/regions}</li>
 *   <li>Wards — {@code GET /authentication/locations?organizationRegionId={regionId}}</li>
 *   <li>Products — {@code GET /products/supplier/{supplierPublicId}}</li>
 * </ul>
 * <p>Pass selected {@code regionId}, {@code locationId}, and {@code productId} into
 * {@link #getMarkers} / {@link #getCustomerDetail}. Markers remain the sole map pin source;
 * detail remains the sole marker-click source.</p>
 */
public interface SupplierPortalCustomerLocationsService {

    CustomerLocationsMarkersV2DTO getMarkers(UUID regionId,
                                             UUID locationId,
                                             UUID productId,
                                             Principal principal);

    CustomerLocationsHeatMapV2DTO getHeatMap(UUID regionId,
                                             UUID locationId,
                                             UUID productId,
                                             CustomerLocationMarkerCategoryV2 category,
                                             Principal principal);

    CustomerLocationDetailV2DTO getCustomerDetail(UUID customerId,
                                                  UUID regionId,
                                                  UUID locationId,
                                                  UUID productId,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  Principal principal);
}
