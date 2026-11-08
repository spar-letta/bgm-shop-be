package com.app.itibari.services.supplierportal;

/**
 * Calendar contract for Supplier Portal trend endpoints (dashboard revenue-trend,
 * product-analysis sales-trend, customer-analysis orders-trend).
 */
public enum SupplierPortalTrendGranularity {

    /** {@code month} + {@code year}: one point per calendar day in that month (zero-filled). */
    DAILY_IN_CALENDAR_MONTH,

    /** {@code year} only: one point per calendar month Jan–Dec (zero-filled). */
    MONTHLY_IN_CALENDAR_YEAR
}
