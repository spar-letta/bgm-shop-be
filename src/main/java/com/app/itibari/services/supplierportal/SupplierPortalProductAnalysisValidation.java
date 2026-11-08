package com.app.itibari.services.supplierportal;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public final class SupplierPortalProductAnalysisValidation {

    static final int MAX_SALES_TREND_DAYS = 366;
    private static final int MAX_WARD_PERFORMANCE_PAGE_SIZE = 100;
    /** Maximum page size accepted for export-only calls (ward-performance, product-ranking-cogs). */
    public static final int EXPORT_MAX_PAGE_SIZE = 2000;
    /** UI table cap for product-ranking-cogs (export uses {@link #EXPORT_MAX_PAGE_SIZE}). */
    public static final int PRODUCT_RANKING_COGS_UI_TOP_N = 10;

    private SupplierPortalProductAnalysisValidation() {
    }

    public static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("startDate/endDate required for Product Analysis V2")
                    .userMessage("Date range required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestRestApiException()
                    .developerMessage("endDate before startDate")
                    .userMessage("Invalid date range");
        }
    }

    public static void validateSalesTrendDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > MAX_SALES_TREND_DAYS) {
            throw new BadRequestRestApiException()
                    .developerMessage("sales-trend range exceeds " + MAX_SALES_TREND_DAYS + " days")
                    .userMessage("Date range too large for sales trend");
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

    public static SupplierPortalTrendPeriod resolveSalesTrendPeriod(Integer month,
                                                                    Integer year,
                                                                    LocalDate today) {
        return SupplierPortalTrendPeriodValidation.resolve(
                month, year, today, "Product Analysis sales-trend");
    }

    /** @deprecated use {@link #resolveSalesTrendPeriod} */
    @Deprecated
    public static LocalDate[] salesTrendWindow(Integer month, Integer year) {
        SupplierPortalTrendPeriod period = resolveSalesTrendPeriod(month, year, LocalDate.now());
        return new LocalDate[] {period.startInclusive(), period.endInclusive()};
    }

    public static void validateSalesCalendarMonthYear(Integer month, Integer year) {
        validateCalendarMonthYear(month, year, "Product Analysis sales-calendar");
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

    /** UI product-ranking-cogs ignores page/size; only rejects client sort. */
    public static void validateProductRankingCogsUiPageable(Pageable pageable) {
        if (!pageable.getSort().isUnsorted()) {
            throw new BadRequestRestApiException()
                    .developerMessage("product-ranking-cogs uses fixed COGS-desc order; omit sort")
                    .userMessage("Invalid sort");
        }
    }

    public static void validateWardPerformancePageable(Pageable pageable) {
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > MAX_WARD_PERFORMANCE_PAGE_SIZE) {
            throw new BadRequestRestApiException()
                    .developerMessage("page size must be between 1 and " + MAX_WARD_PERFORMANCE_PAGE_SIZE)
                    .userMessage("Invalid page size");
        }
        if (pageable.getSort().isUnsorted()) {
            return;
        }
        throw new BadRequestRestApiException()
                .developerMessage("ward-performance uses fixed order: settledTonnes desc, organizationLocationName asc; omit sort")
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
