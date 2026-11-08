package com.app.itibari.services.supplierportal;

import com.app.itibari.dataTypes.CustomerLocationMarkerCategoryV2;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierPortalCustomerLocationsValidationTest {

    @Test
    void validateDetailOptionalDateRange_allowsBothOrNeither() {
        assertDoesNotThrow(() ->
                SupplierPortalCustomerLocationsValidation.validateDetailOptionalDateRange(null, null));
        assertDoesNotThrow(() ->
                SupplierPortalCustomerLocationsValidation.validateDetailOptionalDateRange(
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 31)));
    }

    @Test
    void validateDetailOptionalDateRange_rejectsOneSidedDates() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerLocationsValidation.validateDetailOptionalDateRange(
                        LocalDate.of(2026, 3, 1),
                        null));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerLocationsValidation.validateDetailOptionalDateRange(
                        null,
                        LocalDate.of(2026, 3, 31)));
    }

    @Test
    void maxMarkers_isLockedAt5000() {
        assertEquals(5000, SupplierPortalCustomerLocationsValidation.MAX_MARKERS);
    }

    @Test
    void validateHeatMapCategoryFilter_allowsNullAndHeatMapCategories() {
        assertNull(SupplierPortalCustomerLocationsValidation.validateHeatMapCategoryFilter(null));
        assertEquals(CustomerLocationMarkerCategoryV2.NEW,
                SupplierPortalCustomerLocationsValidation.validateHeatMapCategoryFilter(
                        CustomerLocationMarkerCategoryV2.NEW));
        assertEquals(CustomerLocationMarkerCategoryV2.ACTIVE,
                SupplierPortalCustomerLocationsValidation.validateHeatMapCategoryFilter(
                        CustomerLocationMarkerCategoryV2.ACTIVE));
        assertEquals(CustomerLocationMarkerCategoryV2.CHURNED,
                SupplierPortalCustomerLocationsValidation.validateHeatMapCategoryFilter(
                        CustomerLocationMarkerCategoryV2.CHURNED));
    }

    @Test
    void validateHeatMapCategoryFilter_rejectsInactive() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerLocationsValidation.validateHeatMapCategoryFilter(
                        CustomerLocationMarkerCategoryV2.INACTIVE));
    }
}
