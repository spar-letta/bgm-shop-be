package com.app.itibari.services.supplierportal;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Shared month/year contract for Supplier Portal trend endpoints.
 */
public final class SupplierPortalTrendPeriodValidation {

    private SupplierPortalTrendPeriodValidation() {
    }

    /**
     * Rejects legacy {@code monthly}/{@code quarterly}/{@code yearly} granularity toggles on the two-mode contract.
     */
    public static void rejectLegacyGranularityFlags(Boolean monthly, Boolean quarterly, Boolean yearly) {
        if (Boolean.TRUE.equals(monthly) || Boolean.TRUE.equals(quarterly) || Boolean.TRUE.equals(yearly)) {
            throw new BadRequestRestApiException()
                    .developerMessage("monthly, quarterly, and yearly flags are not supported with the month/year trend contract")
                    .userMessage("Remove monthly, quarterly, and yearly parameters");
        }
    }

    /**
     * @param month optional 1–12; when present selects daily-in-month mode
     * @param year  optional; defaults to {@code today}'s year when omitted
     */
    public static SupplierPortalTrendPeriod resolve(Integer month, Integer year, LocalDate today, String context) {
        if (month != null && year == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("year is required when month is supplied for " + context)
                    .userMessage("Year is required when month is supplied");
        }

        int effectiveYear = year != null ? year : today.getYear();
        validateYear(effectiveYear, context);

        if (month != null) {
            validateMonth(month, context);
            YearMonth calendarMonth = YearMonth.of(effectiveYear, month);
            if (calendarMonth.atDay(1).isAfter(today)) {
                throw new BadRequestRestApiException()
                        .developerMessage(context + " period cannot start in the future")
                        .userMessage("Selected period is in the future");
            }
            return new SupplierPortalTrendPeriod(
                    effectiveYear,
                    month,
                    calendarMonth.atDay(1),
                    calendarMonth.atEndOfMonth(),
                    SupplierPortalTrendGranularity.DAILY_IN_CALENDAR_MONTH);
        }

        if (effectiveYear > today.getYear()) {
            throw new BadRequestRestApiException()
                    .developerMessage(context + " year cannot be in the future")
                    .userMessage("Selected year is in the future");
        }

        return new SupplierPortalTrendPeriod(
                effectiveYear,
                null,
                LocalDate.of(effectiveYear, 1, 1),
                LocalDate.of(effectiveYear, 12, 31),
                SupplierPortalTrendGranularity.MONTHLY_IN_CALENDAR_YEAR);
    }

    public static YearMonth previousCalendarMonth(YearMonth current) {
        return current.minusMonths(1);
    }

    public static LocalDate[] previousCalendarYearWindow(int year) {
        int prevYear = year - 1;
        return new LocalDate[] {
                LocalDate.of(prevYear, 1, 1),
                LocalDate.of(prevYear, 12, 31)
        };
    }

    private static void validateMonth(int month, String context) {
        if (month < 1 || month > 12) {
            throw new BadRequestRestApiException()
                    .developerMessage("month must be between 1 and 12 for " + context)
                    .userMessage("Invalid month");
        }
    }

    private static void validateYear(int year, String context) {
        try {
            YearMonth.of(year, 1);
        } catch (DateTimeException ex) {
            throw new BadRequestRestApiException()
                    .developerMessage("invalid year for " + context + ": " + year)
                    .userMessage("Invalid year");
        }
    }
}
