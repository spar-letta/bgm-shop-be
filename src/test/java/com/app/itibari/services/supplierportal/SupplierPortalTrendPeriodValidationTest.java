package com.app.itibari.services.supplierportal;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierPortalTrendPeriodValidationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 3);

    @Test
    void resolve_defaultsYearToTodayWhenOmitted() {
        SupplierPortalTrendPeriod period = SupplierPortalTrendPeriodValidation.resolve(
                null, null, TODAY, "test");
        assertEquals(2026, period.year());
        assertNull(period.month());
        assertEquals(SupplierPortalTrendGranularity.MONTHLY_IN_CALENDAR_YEAR, period.granularity());
    }

    @Test
    void resolve_monthAndYear_dailyInCalendarMonth() {
        SupplierPortalTrendPeriod period = SupplierPortalTrendPeriodValidation.resolve(
                3, 2025, TODAY, "test");
        assertEquals(2025, period.year());
        assertEquals(3, period.month());
        assertEquals(LocalDate.of(2025, 3, 1), period.startInclusive());
        assertEquals(LocalDate.of(2025, 3, 31), period.endInclusive());
        assertEquals(SupplierPortalTrendGranularity.DAILY_IN_CALENDAR_MONTH, period.granularity());
    }

    @Test
    void resolve_yearOnly_monthlyInCalendarYear() {
        SupplierPortalTrendPeriod period = SupplierPortalTrendPeriodValidation.resolve(
                null, 2025, TODAY, "test");
        assertEquals(2025, period.year());
        assertNull(period.month());
        assertEquals(LocalDate.of(2025, 1, 1), period.startInclusive());
        assertEquals(LocalDate.of(2025, 12, 31), period.endInclusive());
        assertEquals(SupplierPortalTrendGranularity.MONTHLY_IN_CALENDAR_YEAR, period.granularity());
    }

    @Test
    void resolve_rejectsMonthWithoutYear() {
        assertThrows(BadRequestRestApiException.class, () -> SupplierPortalTrendPeriodValidation.resolve(
                6, null, TODAY, "test"));
    }

    @Test
    void rejectLegacyGranularityFlags_rejectsAnyTrueFlag() {
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalTrendPeriodValidation.rejectLegacyGranularityFlags(true, false, false));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalTrendPeriodValidation.rejectLegacyGranularityFlags(false, true, false));
        assertThrows(BadRequestRestApiException.class, () ->
                SupplierPortalTrendPeriodValidation.rejectLegacyGranularityFlags(false, false, true));
    }
}
