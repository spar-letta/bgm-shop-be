package com.app.itibari.services.supplierportal;

import com.app.itibari.dataTypes.CustomerLocationMarkerCategoryV2;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;

import java.time.LocalDate;

public final class SupplierPortalCustomerLocationsValidation {

    public static final int MAX_MARKERS = 5000;

    /**
     * Geohash precision for heat-map cell aggregation (~4.9 km × 4.9 km at equator).
     * Exact customer coordinates stay in the nightly snapshot; only cell centers leave the API.
     */
    public static final int HEAT_MAP_GEOHASH_PRECISION = 5;

    private SupplierPortalCustomerLocationsValidation() {
    }

    /**
     * Optional heat-map {@code category} query param. {@code null} means no filter (all heat-map
     * categories). {@link CustomerLocationMarkerCategoryV2#INACTIVE} and any other non-heat-map
     * value are rejected with 400.
     */
    public static CustomerLocationMarkerCategoryV2 validateHeatMapCategoryFilter(
            CustomerLocationMarkerCategoryV2 category) {
        if (category == null) {
            return null;
        }
        if (!SupplierPortalCustomerLocationsSupport.isHeatMapCategory(category)) {
            throw new BadRequestRestApiException()
                    .developerMessage("customer-locations heat-map category must be NEW, ACTIVE, or CHURNED; got "
                            + category)
                    .userMessage("Invalid heat map category");
        }
        return category;
    }

    /**
     * Detail endpoint: omit both dates (no periodMetrics), or supply both.
     * One-sided date input is rejected with 400.
     */
    public static void validateDetailOptionalDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return;
        }
        if (startDate == null || endDate == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("customer-locations detail requires both startDate and endDate when either is supplied")
                    .userMessage("Both start date and end date are required for period metrics");
        }
        validateRequiredDateRange(startDate, endDate, "customer-locations detail period metrics");
    }

    private static void validateRequiredDateRange(LocalDate startDate, LocalDate endDate, String context) {
        if (startDate == null || endDate == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("startDate/endDate required for " + context)
                    .userMessage("Date range required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestRestApiException()
                    .developerMessage("endDate before startDate for " + context)
                    .userMessage("Invalid date range");
        }
    }
}
