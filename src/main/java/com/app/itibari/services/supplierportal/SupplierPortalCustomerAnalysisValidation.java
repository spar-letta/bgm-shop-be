package com.app.itibari.services.supplierportal;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

public final class SupplierPortalCustomerAnalysisValidation {

    public static final int DEFAULT_DISTRICT_RANKING_LIMIT = 10;
    public static final int MAX_CUSTOMERS_PAGE_SIZE = 100;
    /** Maximum page size accepted for export-only calls (customers). */
    public static final int EXPORT_MAX_PAGE_SIZE = 2000;

    private SupplierPortalCustomerAnalysisValidation() {
    }

    /**
     * Blank or absent query params (e.g. {@code regionId=} from the SPA) become {@code null}.
     */
    public static UUID parseOptionalUuid(String raw, String paramName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid " + paramName + ": " + raw)
                    .userMessage("Invalid filter");
        }
    }

    public static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("startDate/endDate required for Customer Analysis V2")
                    .userMessage("Date range required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestRestApiException()
                    .developerMessage("endDate before startDate")
                    .userMessage("Invalid date range");
        }
    }

    public static SupplierPortalTrendPeriod resolveOrdersTrendPeriod(Integer month,
                                                                     Integer year,
                                                                     LocalDate today) {
        return SupplierPortalTrendPeriodValidation.resolve(
                month, year, today, "Customer Analysis orders-trend");
    }

    /** @deprecated use {@link #resolveOrdersTrendPeriod} */
    @Deprecated
    public static LocalDate[] ordersTrendWindow(Integer month, Integer year) {
        SupplierPortalTrendPeriod period = resolveOrdersTrendPeriod(month, year, LocalDate.now());
        return new LocalDate[] {period.startInclusive(), period.endInclusive()};
    }

    public static void validateActivityCalendarMonthYear(Integer month, Integer year) {
        validateCalendarMonthYear(month, year, "Customer Analysis activity-calendar");
    }

    private static void validateCalendarMonthYear(Integer month, Integer year, String context) {
        if (month == null || year == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("month and year required for " + context)
                    .userMessage("Month and year are required");
        }
        if (month < 1 || month > 12) {
            throw new BadRequestRestApiException()
                    .developerMessage("month must be between 1 and 12")
                    .userMessage("Invalid month");
        }
        try {
            YearMonth.of(year, month);
        } catch (RuntimeException ex) {
            throw new BadRequestRestApiException()
                    .developerMessage("invalid calendar month/year: " + month + "/" + year)
                    .userMessage("Invalid month or year");
        }
    }

    public static void validateDistrictRankingLimit(Integer limit) {
        if (limit == null) {
            return;
        }
        if (limit < 1 || limit > 100) {
            throw new BadRequestRestApiException()
                    .developerMessage("limit must be between 1 and 100")
                    .userMessage("Invalid limit");
        }
    }

    /**
     * Applies {@link #DEFAULT_DISTRICT_RANKING_LIMIT} when {@code limit} is omitted, then validates bounds.
     */
    public static int effectiveDistrictRankingLimit(Integer limit) {
        int effective = limit != null ? limit : DEFAULT_DISTRICT_RANKING_LIMIT;
        validateDistrictRankingLimit(effective);
        return effective;
    }

    /**
     * Customers table uses fixed SQL order ({@code totalCogsTzs DESC, shopName ASC}).
     * Rejects client {@code sort} query params and caps page size at {@link #MAX_CUSTOMERS_PAGE_SIZE}.
     */
    public static void validateCustomersPageable(Pageable pageable) {
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > MAX_CUSTOMERS_PAGE_SIZE) {
            throw new BadRequestRestApiException()
                    .developerMessage("page size must be between 1 and " + MAX_CUSTOMERS_PAGE_SIZE)
                    .userMessage("Invalid page size");
        }
        if (pageable.getSort().isUnsorted()) {
            return;
        }
        throw new BadRequestRestApiException()
                .developerMessage("customers table uses fixed order: totalCogsTzs desc, shopName asc; omit sort")
                .userMessage("Invalid sort");
    }

    /**
     * Validates a pageable for export-only calls that need a larger page-size cap.
     * Exports use fixed-order queries, so sort is always rejected.
     */
    public static void validateExportPageable(Pageable pageable) {
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > EXPORT_MAX_PAGE_SIZE) {
            throw new BadRequestRestApiException()
                    .developerMessage("export page size must be between 1 and " + EXPORT_MAX_PAGE_SIZE)
                    .userMessage("Invalid page size");
        }
        if (!pageable.getSort().isUnsorted()) {
            throw new BadRequestRestApiException()
                    .developerMessage("export uses fixed order; omit sort")
                    .userMessage("Invalid sort");
        }
    }
}
