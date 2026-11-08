package com.app.itibari.services.supplierportal;

import java.time.LocalDate;

/**
 * Resolved inclusive date window for a trend request.
 *
 * @param year         calendar year (defaults to current year when omitted at validation)
 * @param month        1–12 when {@link SupplierPortalTrendGranularity#DAILY_IN_CALENDAR_MONTH}; null for year-only
 * @param startInclusive first day in window
 * @param endInclusive   last day in window
 * @param granularity    daily-in-month vs monthly-in-year
 */
public record SupplierPortalTrendPeriod(
        int year,
        Integer month,
        LocalDate startInclusive,
        LocalDate endInclusive,
        SupplierPortalTrendGranularity granularity) {
}
