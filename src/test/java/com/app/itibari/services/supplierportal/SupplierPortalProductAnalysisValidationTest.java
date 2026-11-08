package com.app.itibari.services.supplierportal;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierPortalProductAnalysisValidationTest {

    @Test
    void validateSalesTrendDateRange_rejectsMoreThan366Days() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = start.plusDays(SupplierPortalProductAnalysisValidation.MAX_SALES_TREND_DAYS);

        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.validateSalesTrendDateRange(start, end));
    }

    @Test
    void validateSalesTrendDateRange_allowsExactly366Days() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = start.plusDays(SupplierPortalProductAnalysisValidation.MAX_SALES_TREND_DAYS - 1);

        assertDoesNotThrow(() ->
                SupplierPortalProductAnalysisValidation.validateSalesTrendDateRange(start, end));
    }

    @Test
    void validateDateRange_rejectsEndBeforeStart() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.validateDateRange(
                        LocalDate.of(2026, 3, 10),
                        LocalDate.of(2026, 3, 9)));
    }

    @Test
    void validateDistrictRankingLimit_rejectsOutOfRangeValues() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.validateDistrictRankingLimit(0));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.validateDistrictRankingLimit(101));
    }

    @Test
    void validateDistrictRankingLimit_allowsNullAndInRangeValues() {
        assertDoesNotThrow(() -> SupplierPortalProductAnalysisValidation.validateDistrictRankingLimit(null));
        assertDoesNotThrow(() -> SupplierPortalProductAnalysisValidation.validateDistrictRankingLimit(1));
        assertDoesNotThrow(() -> SupplierPortalProductAnalysisValidation.validateDistrictRankingLimit(100));
    }

    @Test
    void validateWardPerformancePageable_rejectsOversizedPage() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.validateWardPerformancePageable(
                        PageRequest.of(0, 101)));
    }

    @Test
    void validateWardPerformancePageable_rejectsAnyClientSort() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.validateWardPerformancePageable(
                        PageRequest.of(0, 20, Sort.by(Sort.Order.asc("settledTonnes")))));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.validateWardPerformancePageable(
                        PageRequest.of(0, 20, Sort.by(Sort.Order.desc("organizationLocationName")))));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.validateWardPerformancePageable(
                        PageRequest.of(0, 20, Sort.by(Sort.Order.desc("totalCogsTzs")))));
    }

    @Test
    void validateWardPerformancePageable_allowsUnsortedPageable() {
        assertDoesNotThrow(() ->
                SupplierPortalProductAnalysisValidation.validateWardPerformancePageable(PageRequest.of(0, 20)));
    }

    @Test
    void resolveSalesTrendPeriod_rejectsMonthWithoutYearAndInvalidMonth() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.resolveSalesTrendPeriod(
                        3, null, java.time.LocalDate.of(2026, 6, 3)));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.resolveSalesTrendPeriod(
                        0, 2026, java.time.LocalDate.of(2026, 6, 3)));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.resolveSalesTrendPeriod(
                        13, 2026, java.time.LocalDate.of(2026, 6, 3)));
    }

    @Test
    void resolveSalesTrendPeriod_allowsYearOnly() {
        assertDoesNotThrow(() ->
                SupplierPortalProductAnalysisValidation.resolveSalesTrendPeriod(
                        null, 2026, java.time.LocalDate.of(2026, 6, 3)));
    }

    @Test
    void salesTrendWindow_mapsMonthYearToFirstAndLastDayOfMonth() {
        LocalDate[] window = SupplierPortalProductAnalysisValidation.salesTrendWindow(3, 2026);

        assertEquals(LocalDate.of(2026, 3, 1), window[0]);
        assertEquals(LocalDate.of(2026, 3, 31), window[1]);
    }

    @Test
    void validateSalesCalendarMonthYear_rejectsMissingOrInvalidMonth() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.validateSalesCalendarMonthYear(null, 2026));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.validateSalesCalendarMonthYear(3, null));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.validateSalesCalendarMonthYear(0, 2026));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalProductAnalysisValidation.validateSalesCalendarMonthYear(13, 2026));
    }

    @Test
    void validateSalesCalendarMonthYear_allowsValidMonthYear() {
        assertDoesNotThrow(() ->
                SupplierPortalProductAnalysisValidation.validateSalesCalendarMonthYear(2, 2026));
    }
}
