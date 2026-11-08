package com.app.itibari.services.supplierportal;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Request-shape validation for Supplier Portal V2 dashboard APIs.
 */
public final class SupplierPortalDashboardValidation {

    private SupplierPortalDashboardValidation() {
    }

    /**
     * Optional revenue-trend period filter: {@code month} and {@code year} must be supplied together.
     * {@code year} selects the chart calendar year; {@code month} selects the YoY comparison month.
     * When present, the selected calendar month must not start after {@code today}.
     */
    public static void validateRevenueTrendPeriodFilter(Integer month, Integer year, LocalDate today) {
        if (month == null && year == null) {
            return;
        }
        if (month == null || year == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("month and year must both be supplied for revenue-trend period filtering")
                    .userMessage("Month and year are required together");
        }
        if (month < 1 || month > 12) {
            throw new BadRequestRestApiException()
                    .developerMessage("month must be between 1 and 12")
                    .userMessage("Invalid month");
        }
        YearMonth selected = YearMonth.of(year, month);
        if (selected.atDay(1).isAfter(today)) {
            throw new BadRequestRestApiException()
                    .developerMessage("revenue-trend period cannot start in the future")
                    .userMessage("Selected period is in the future");
        }
    }

    /**
     * Sales calendar month/year: {@code month} must be 1–12 and form a valid {@link YearMonth} with {@code year}.
     */
    public static void validateCalendarMonthYear(int month, int year) {
        if (month < 1 || month > 12) {
            throw new BadRequestRestApiException()
                    .developerMessage("month must be between 1 and 12, got " + month)
                    .userMessage("Invalid month");
        }
        try {
            YearMonth.of(year, month);
        } catch (DateTimeException ex) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid calendar month/year: " + month + "/" + year)
                    .userMessage("Invalid month or year");
        }
    }

    /**
     * Revenue trend granularity: at most one of {@code monthly}, {@code quarterly}, {@code yearly} may be {@code true}.
     * Zero flags true keeps service default (monthly buckets).
     */
    public static void validateRevenueTrendGranularityFlags(Boolean monthly, Boolean quarterly, Boolean yearly) {
        int trueFlags = 0;
        if (Boolean.TRUE.equals(monthly)) {
            trueFlags++;
        }
        if (Boolean.TRUE.equals(quarterly)) {
            trueFlags++;
        }
        if (Boolean.TRUE.equals(yearly)) {
            trueFlags++;
        }
        if (trueFlags > 1) {
            throw new BadRequestRestApiException()
                    .developerMessage("At most one revenue-trend granularity flag (monthly, quarterly, yearly) may be true")
                    .userMessage("Choose only one trend granularity");
        }
    }
}
