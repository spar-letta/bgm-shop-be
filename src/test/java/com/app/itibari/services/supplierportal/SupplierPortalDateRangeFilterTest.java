package com.app.itibari.services.supplierportal;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierPortalDateRangeFilterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 16);

    @Test
    void resolve_explicitStartEnd_unchanged() {
        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);
        SupplierPortalDateRangeFilter.ResolvedDateRange range = SupplierPortalDateRangeFilter.resolve(
                start, end, null, null, null, TODAY, "test");
        assertEquals(start, range.startInclusive());
        assertEquals(end, range.endInclusive());
    }

    @Test
    void resolve_yearAndQuarter_secondQuarterAprToJun() {
        SupplierPortalDateRangeFilter.ResolvedDateRange range = SupplierPortalDateRangeFilter.resolve(
                null, null, 2026, null, 2, TODAY, "test");
        assertEquals(LocalDate.of(2026, 4, 1), range.startInclusive());
        assertEquals(LocalDate.of(2026, 6, 30), range.endInclusive());
    }

    @Test
    void resolve_yearAndMonth_fullCalendarMonth() {
        SupplierPortalDateRangeFilter.ResolvedDateRange range = SupplierPortalDateRangeFilter.resolve(
                null, null, 2026, 2, null, TODAY, "test");
        assertEquals(LocalDate.of(2026, 2, 1), range.startInclusive());
        assertEquals(LocalDate.of(2026, 2, 28), range.endInclusive());
    }

    @Test
    void resolve_yearOnly_fullCalendarYear() {
        SupplierPortalDateRangeFilter.ResolvedDateRange range = SupplierPortalDateRangeFilter.resolve(
                null, null, 2025, null, null, TODAY, "test");
        assertEquals(LocalDate.of(2025, 1, 1), range.startInclusive());
        assertEquals(LocalDate.of(2025, 12, 31), range.endInclusive());
    }

    @Test
    void resolve_rejectsMixedExplicitAndCalendar() {
        assertThrows(BadRequestRestApiException.class, () -> SupplierPortalDateRangeFilter.resolve(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 2026, null, 2, TODAY, "test"));
    }

    @Test
    void resolve_rejectsMonthAndQuarterTogether() {
        assertThrows(BadRequestRestApiException.class, () -> SupplierPortalDateRangeFilter.resolve(
                null, null, 2026, 4, 2, TODAY, "test"));
    }

    @Test
    void resolve_rejectsQuarterWithoutYear() {
        assertThrows(BadRequestRestApiException.class, () -> SupplierPortalDateRangeFilter.resolve(
                null, null, null, null, 2, TODAY, "test"));
    }

    @Test
    void resolve_rejectsMissingFilters() {
        assertThrows(BadRequestRestApiException.class, () -> SupplierPortalDateRangeFilter.resolve(
                null, null, null, null, null, TODAY, "test"));
    }
}
