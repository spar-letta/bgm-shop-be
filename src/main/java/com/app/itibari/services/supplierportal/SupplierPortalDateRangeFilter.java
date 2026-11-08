package com.app.itibari.services.supplierportal;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Resolves inclusive {@code startDate}/{@code endDate} for Supplier Portal analytics from either
 * explicit date bounds or calendar filters ({@code year}, optional {@code month} or {@code quarter}).
 */
public final class SupplierPortalDateRangeFilter {

    public record ResolvedDateRange(LocalDate startInclusive, LocalDate endInclusive) {
    }

    private SupplierPortalDateRangeFilter() {
    }

    /**
     * @param quarter query param — calendar quarter 1–4 (Q1 Jan–Mar … Q4 Oct–Dec)
     */
    public static ResolvedDateRange resolve(LocalDate startDate,
                                            LocalDate endDate,
                                            Integer year,
                                            Integer month,
                                            Integer quarter,
                                            LocalDate today,
                                            String context) {
        boolean hasCalendar = year != null || month != null || quarter != null;
        boolean hasExplicit = startDate != null || endDate != null;

        if (hasCalendar && hasExplicit) {
            throw new BadRequestRestApiException()
                    .developerMessage("Use either startDate/endDate or year/month/quarter for " + context + ", not both")
                    .userMessage("Choose either a date range or a calendar filter");
        }

        if (hasCalendar) {
            return resolveCalendarWindow(year, month, quarter, today, context);
        }

        if (startDate == null || endDate == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("startDate/endDate or year/month/quarter required for " + context)
                    .userMessage("Date range required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestRestApiException()
                    .developerMessage("endDate before startDate for " + context)
                    .userMessage("Invalid date range");
        }
        return new ResolvedDateRange(startDate, endDate);
    }

    private static ResolvedDateRange resolveCalendarWindow(Integer year,
                                                           Integer month,
                                                           Integer quarter,
                                                           LocalDate today,
                                                           String context) {
        if (month != null && quarter != null) {
            throw new BadRequestRestApiException()
                    .developerMessage("month and quarter are mutually exclusive for " + context)
                    .userMessage("Choose either month or quarter, not both");
        }
        if (month != null && year == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("year is required when month is supplied for " + context)
                    .userMessage("Year is required when month is supplied");
        }
        if (quarter != null && year == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("year is required when quarter is supplied for " + context)
                    .userMessage("Year is required when quarter is supplied");
        }

        int effectiveYear = year != null ? year : today.getYear();
        validateYear(effectiveYear, context);

        if (month != null) {
            validateMonth(month, context);
            YearMonth calendarMonth = YearMonth.of(effectiveYear, month);
            rejectFutureStart(calendarMonth.atDay(1), today, context);
            return new ResolvedDateRange(calendarMonth.atDay(1), calendarMonth.atEndOfMonth());
        }

        if (quarter != null) {
            validateQuarter(quarter, context);
            LocalDate quarterStart = quarterStart(effectiveYear, quarter);
            LocalDate quarterEnd = quarterEnd(effectiveYear, quarter);
            rejectFutureStart(quarterStart, today, context);
            return new ResolvedDateRange(quarterStart, quarterEnd);
        }

        if (effectiveYear > today.getYear()) {
            throw new BadRequestRestApiException()
                    .developerMessage(context + " year cannot be in the future")
                    .userMessage("Selected year is in the future");
        }
        return new ResolvedDateRange(
                LocalDate.of(effectiveYear, 1, 1),
                LocalDate.of(effectiveYear, 12, 31));
    }

    static LocalDate quarterStart(int year, int quarter) {
        int startMonth = (quarter - 1) * 3 + 1;
        return LocalDate.of(year, startMonth, 1);
    }

    static LocalDate quarterEnd(int year, int quarter) {
        int endMonth = quarter * 3;
        return YearMonth.of(year, endMonth).atEndOfMonth();
    }

    private static void rejectFutureStart(LocalDate periodStart, LocalDate today, String context) {
        if (periodStart.isAfter(today)) {
            throw new BadRequestRestApiException()
                    .developerMessage(context + " period cannot start in the future")
                    .userMessage("Selected period is in the future");
        }
    }

    private static void validateMonth(int month, String context) {
        if (month < 1 || month > 12) {
            throw new BadRequestRestApiException()
                    .developerMessage("month must be between 1 and 12 for " + context)
                    .userMessage("Invalid month");
        }
    }

    private static void validateQuarter(int quarter, String context) {
        if (quarter < 1 || quarter > 4) {
            throw new BadRequestRestApiException()
                    .developerMessage("quarter must be between 1 and 4 for " + context)
                    .userMessage("Invalid quarter");
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
