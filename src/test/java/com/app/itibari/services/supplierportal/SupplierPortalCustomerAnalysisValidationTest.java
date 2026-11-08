package com.app.itibari.services.supplierportal;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierPortalCustomerAnalysisValidationTest {

    @Test
    void validateDateRange_rejectsMissingDates() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.validateDateRange(null, LocalDate.of(2026, 3, 31)));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.validateDateRange(LocalDate.of(2026, 3, 1), null));
    }

    @Test
    void validateDateRange_rejectsEndBeforeStart() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.validateDateRange(
                        LocalDate.of(2026, 3, 10),
                        LocalDate.of(2026, 3, 9)));
    }

    @Test
    void validateDistrictRankingLimit_rejectsOutOfRangeValues() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.validateDistrictRankingLimit(0));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.validateDistrictRankingLimit(101));
    }

    @Test
    void validateDistrictRankingLimit_allowsNullAndInRangeValues() {
        assertDoesNotThrow(() -> SupplierPortalCustomerAnalysisValidation.validateDistrictRankingLimit(null));
        assertDoesNotThrow(() -> SupplierPortalCustomerAnalysisValidation.validateDistrictRankingLimit(10));
        assertDoesNotThrow(() -> SupplierPortalCustomerAnalysisValidation.validateDistrictRankingLimit(100));
    }

    @Test
    void validateCustomersPageable_rejectsOversizedPage() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.validateCustomersPageable(PageRequest.of(0, 101)));
    }

    @Test
    void validateCustomersPageable_rejectsAnyClientSort() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.validateCustomersPageable(
                        PageRequest.of(0, 20, Sort.by(Sort.Order.desc("totalCogsTzs")))));
    }

    @Test
    void validateCustomersPageable_allowsUnsortedPageable() {
        assertDoesNotThrow(() ->
                SupplierPortalCustomerAnalysisValidation.validateCustomersPageable(PageRequest.of(0, 20)));
    }

    @Test
    void effectiveDistrictRankingLimit_appliesDefaultAndValidates() {
        assertEquals(10, SupplierPortalCustomerAnalysisValidation.effectiveDistrictRankingLimit(null));
        assertEquals(25, SupplierPortalCustomerAnalysisValidation.effectiveDistrictRankingLimit(25));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.effectiveDistrictRankingLimit(0));
    }

    @Test
    void ordersTrendWindow_mapsMonthYearToFirstAndLastDayOfMonth() {
        LocalDate[] window = SupplierPortalCustomerAnalysisValidation.ordersTrendWindow(6, 2026);
        assertEquals(LocalDate.of(2026, 6, 1), window[0]);
        assertEquals(LocalDate.of(2026, 6, 30), window[1]);
    }

    @Test
    void resolveOrdersTrendPeriod_rejectsMonthWithoutYearAndInvalidMonth() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.resolveOrdersTrendPeriod(
                        6, null, java.time.LocalDate.of(2026, 6, 3)));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.resolveOrdersTrendPeriod(
                        0, 2026, java.time.LocalDate.of(2026, 6, 3)));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.resolveOrdersTrendPeriod(
                        13, 2026, java.time.LocalDate.of(2026, 6, 3)));
    }

    @Test
    void resolveOrdersTrendPeriod_allowsYearOnlyAndMonthYear() {
        assertDoesNotThrow(() ->
                SupplierPortalCustomerAnalysisValidation.resolveOrdersTrendPeriod(
                        null, 2026, java.time.LocalDate.of(2026, 6, 3)));
        assertDoesNotThrow(() ->
                SupplierPortalCustomerAnalysisValidation.resolveOrdersTrendPeriod(
                        6, 2026, java.time.LocalDate.of(2026, 6, 3)));
    }

    @Test
    void validateActivityCalendarMonthYear_rejectsMissingOrInvalidMonth() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.validateActivityCalendarMonthYear(null, 2026));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.validateActivityCalendarMonthYear(3, null));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.validateActivityCalendarMonthYear(0, 2026));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.validateActivityCalendarMonthYear(13, 2026));
    }

    @Test
    void validateActivityCalendarMonthYear_allowsValidMonthYear() {
        assertDoesNotThrow(() ->
                SupplierPortalCustomerAnalysisValidation.validateActivityCalendarMonthYear(2, 2026));
    }

    @Test
    void parseOptionalUuid_blankValuesBecomeNull() {
        assertNull(SupplierPortalCustomerAnalysisValidation.parseOptionalUuid(null, "regionId"));
        assertNull(SupplierPortalCustomerAnalysisValidation.parseOptionalUuid("", "regionId"));
        assertNull(SupplierPortalCustomerAnalysisValidation.parseOptionalUuid("   ", "locationId"));
    }

    @Test
    void parseOptionalUuid_parsesTrimmedUuid() {
        UUID id = UUID.randomUUID();
        assertEquals(id, SupplierPortalCustomerAnalysisValidation.parseOptionalUuid("  " + id + "  ", "regionId"));
    }

    @Test
    void parseOptionalUuid_rejectsInvalidUuid() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalCustomerAnalysisValidation.parseOptionalUuid("not-a-uuid", "regionId"));
    }
}
