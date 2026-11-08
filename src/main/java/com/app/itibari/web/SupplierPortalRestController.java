package com.app.itibari.web;

import com.app.itibari.dataTypes.CustomerLocationMarkerCategoryV2;
import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.dataTypes.SupplierPortalCustomerAnalysisView;
import com.app.itibari.dataTypes.SupplierPortalProductAnalysisView;
import com.app.itibari.models.entities.OrganizationLocations;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.utils.reportHelper.SupplierPortalAnalysisExportHelper;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.report.*;
import com.app.itibari.models.response.*;
import com.app.itibari.services.ReportService;
import com.app.itibari.services.SalesOrdersService;
import com.app.itibari.services.SupplierPortalCustomerAnalysisService;
import com.app.itibari.services.SupplierPortalCustomerLocationsService;
import com.app.itibari.services.SupplierPortalDashboardService;
import com.app.itibari.services.SupplierPortalOrganizationLocationsService;
import com.app.itibari.services.SupplierPortalProductAnalysisService;
import com.app.itibari.services.supplierportal.SupplierPortalGeographyQueryParams;
import com.app.itibari.services.supplierportal.SupplierPortalDateRangeFilter;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import com.itextpdf.io.source.ByteArrayOutputStream;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/supplier-portal")
@Tag(name = "Supplier Portal", description = "Supplier Portal dashboard and analytics API")
public class SupplierPortalRestController {

    private static final ZoneId EAST_AFRICA = ZoneId.of("Africa/Dar_es_Salaam");

    private final ReportService reportService;
    private final SalesOrdersService salesOrdersService;
    private final SupplierPortalDashboardService supplierPortalDashboardService;
    private final SupplierPortalProductAnalysisService supplierPortalProductAnalysisService;
    private final SupplierPortalCustomerAnalysisService supplierPortalCustomerAnalysisService;
    private final SupplierPortalCustomerLocationsService supplierPortalCustomerLocationsService;
    private final SupplierPortalOrganizationLocationsService supplierPortalOrganizationLocationsService;
    private final SupplierPortalAnalysisExportHelper analysisExportHelper;

    public SupplierPortalRestController(ReportService reportService,
                                        SalesOrdersService salesOrdersService,
                                        SupplierPortalDashboardService supplierPortalDashboardService,
                                        SupplierPortalProductAnalysisService supplierPortalProductAnalysisService,
                                        SupplierPortalCustomerAnalysisService supplierPortalCustomerAnalysisService,
                                        SupplierPortalCustomerLocationsService supplierPortalCustomerLocationsService,
                                        SupplierPortalOrganizationLocationsService supplierPortalOrganizationLocationsService,
                                        SupplierPortalAnalysisExportHelper analysisExportHelper) {
        this.reportService = reportService;
        this.salesOrdersService = salesOrdersService;
        this.supplierPortalDashboardService = supplierPortalDashboardService;
        this.supplierPortalProductAnalysisService = supplierPortalProductAnalysisService;
        this.supplierPortalCustomerAnalysisService = supplierPortalCustomerAnalysisService;
        this.supplierPortalCustomerLocationsService = supplierPortalCustomerLocationsService;
        this.supplierPortalOrganizationLocationsService = supplierPortalOrganizationLocationsService;
        this.analysisExportHelper = analysisExportHelper;
    }

    @GetMapping("/dashboard/analysis")
    @Operation(summary = "Supplier dashboard analysis", description = "Same payload as GET /reports/supplierDashboardAnalysis",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public SupplierDashboardAnalysis supplierDashboardAnalysis(@RequestParam(name = "sku", required = false) String sku,
                                                               @RequestParam(name = "region", required = false) UUID regionPublicId,
                                                               @RequestParam(name = "location", required = false) UUID locationPublicId,
                                                               @RequestParam(name = "startDate", required = false) LocalDate startDate,
                                                               @RequestParam(name = "endDate", required = false) LocalDate endDate,
                                                               @Parameter(hidden = true) Principal principal,
                                                               @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        return reportService.getSupplierDashboardAnalysis(sku,
                regionPublicId,
                locationPublicId,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    @GetMapping("/dashboard/revenue-aggregate")
    @Operation(summary = "Supplier revenue aggregate report", description = "Same as GET /reports/supplierRevenueReport (Map aggregate)",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Map<String, BigDecimal> supplierRevenueAggregate(
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        return reportService.runSupplierRevenueReport(
                regionId,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    @GetMapping("/dashboard/summary-metrics")
    @Operation(summary = "Supplier dashboard summary cards (V2)",
            description = "Returns four trend metrics (active customers, orders, tonnes sold, COGS) with prior-window deltas. "
                    + "activeCustomers = COUNT(DISTINCT customers) with at least one DELIVERED supplier line "
                    + "(quantity_delivered > 0) in the window — identical semantics to "
                    + "/product-analysis/all-products/summary-metrics activeCustomers and "
                    + "/customer-analysis/summary-metrics customers for the same date + geography filters. "
                    + "COGS = Σ(product.buying_price × quantity_delivered).",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public SummaryCardsV2DTO supplierDashboardSummaryMetricsV2(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @Parameter(hidden = true) Principal principal) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "dashboard summary-metrics");
        return supplierPortalDashboardService.getDashboardSummaryMetrics(
                regionIds(queryParams), locationIds(queryParams), range.startInclusive(), range.endInclusive(), principal);
    }

    @GetMapping("/dashboard/revenue-trend")
    @Operation(summary = "Supplier sales trend (V2)",
            description = "Delivered-order COGS trend (Σ product.buying_price × quantity_delivered). Ignores dashboard startDate/endDate — use month/year only. "
                    + "Mode A: month (1–12) + year → daily zero-filled series for that calendar month; headline = month total vs previous calendar month. "
                    + "Mode B: year only (month omitted; defaults to current year when year omitted) → 12 monthly buckets Jan–Dec; headline = year total vs prior calendar year. "
                    + "month without year is invalid. monthly/quarterly/yearly query flags return 400.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public SalesTrendV2DTO supplierRevenueTrend(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "monthly", required = false) Boolean monthly,
            @RequestParam(name = "quarterly", required = false) Boolean quarterly,
            @RequestParam(name = "yearly", required = false) Boolean yearly,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        return supplierPortalDashboardService.getSalesTrend(
                regionIds(queryParams),
                locationIds(queryParams),
                month,
                year,
                monthly,
                quarterly,
                yearly,
                principal,
                pageable);
    }

    @GetMapping("/dashboard/product-distribution-v2")
    @Operation(summary = "Supplier product distribution (V2)",
            description = "Delivered aggregates per supplier SKU — settled tonnes via product.weight × quantity (weight stored as tonnes); rank spans pages using fixed descending unitsSold order. "
                    + "Each row includes periodTrends (unitsSoldTrendPercentage vs the immediately preceding window of equal inclusive day count; trendLabel is \"vs Previous Period\").",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ProductDistributionV2RowDTO> supplierDashboardProductDistributionV2(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100) Pageable pageable) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "dashboard product-distribution-v2");
        return supplierPortalDashboardService.getProductDistributionV2(
                regionIds(queryParams),
                locationIds(queryParams),
                range.startInclusive(),
                range.endInclusive(),
                principal,
                pageable);
    }

    @GetMapping("/dashboard/district-performance-v2")
    @Operation(summary = "Supplier district-style performance (V2)",
            description = "Paged delivered aggregates with prior-window periodTrends (vs Previous Period; equal inclusive day count before startDate). "
                    + "Default page size 10. Ordered by settled tonnes descending at DB level. "
                    + "totalSupplierLineRevenueTzs is full-window portal COGS across all buckets (not page-limited). "
                    + "District mode (no regionId and no locationId): one row per organization_regions (district); "
                    + "organizationLocationPublicId and organizationLocationName carry region publicId and name. "
                    + "Ward mode (regionId and/or locationId present): rows grouped by customer.organization_location (ward); "
                    + "regionId returns wards within that district, locationId filters to that ward.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public DistrictPerformanceV2PageDTO supplierDashboardDistrictPerformanceV2(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "dashboard district-performance-v2");
        return supplierPortalDashboardService.getDistrictPerformanceV2(
                regionIds(queryParams),
                locationIds(queryParams),
                range.startInclusive(),
                range.endInclusive(),
                principal,
                pageable);
    }

    @GetMapping("/dashboard/sales-heat-map")
    @Operation(summary = "Supplier sales heat map (V2)",
            description = "Calendar-month delivered-order aggregates by day with zero-filled days, heat intensity, and summary cards.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public SalesCalendarV2DTO supplierDashboardSalesHeatMap(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam MultiValueMap<String, String> queryParams,
            @Parameter(hidden = true) Principal principal) {
        return supplierPortalDashboardService.getSalesCalendar(regionIds(queryParams), locationIds(queryParams), month, year, principal);
    }

    @GetMapping("/product-analysis/summary-metrics")
    @Operation(summary = "Product Analysis summary cards (V2)",
            description = "Product-scoped summary metrics with prior-window trends. Cards: activeCustomers (Customers), "
                    + "totalQuantitySoldTonnes (Total Units Sold), totalCogsTzs (COGS), averageOrderValueTzs (AOV = portal COGS ÷ delivered orders). "
                    + "activeCustomers counts DISTINCT customers with DELIVERED lines for this product only — "
                    + "may be lower than supplier-wide totals on Dashboard / all-products / customer-analysis. "
                    + "totalOrders is retained for compatibility. Also returns productImageUrl (when available) and ranking "
                    + "(1-based best-selling rank by portal COGS among supplier products in the same window as product-ranking-cogs; "
                    + "null if the product has no qualifying delivered activity). "
                    + "COGS = Σ(product.buying_price × quantity_delivered). Quantity = settled tonnes (product.weight × quantity_delivered / 1000). "
                    + "Ward/district widgets group by customer.organization_location; regionId filters only.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ProductAnalysisSummaryMetricsV2DTO productAnalysisSummaryMetricsV2(
            @RequestParam(name = "productId") UUID productId,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @Parameter(hidden = true) Principal principal) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "product-analysis summary-metrics");
        return supplierPortalProductAnalysisService.getSummaryMetrics(
                productId, regionIds(queryParams), locationIds(queryParams), range.startInclusive(), range.endInclusive(), principal);
    }

    @GetMapping("/product-analysis/sales-trend")
    @Operation(summary = "Product Analysis sales trend (V2)",
            description = "Ignores startDate/endDate. Mode A: month + year → daily zero-filled series for one product "
                    + "(totalCogsTzs, orderCount, customerCount); headline = month COGS vs previous calendar month. "
                    + "Mode B: year only → 12 monthly points; headline = year COGS vs prior calendar year. "
                    + "month without year is invalid.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ProductAnalysisSalesTrendV2DTO productAnalysisSalesTrendV2(
            @RequestParam(name = "productId") UUID productId,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam MultiValueMap<String, String> queryParams,
            @Parameter(hidden = true) Principal principal) {
        return supplierPortalProductAnalysisService.getSalesTrend(
                productId, regionIds(queryParams), locationIds(queryParams), month, year, principal);
    }

    @GetMapping("/product-analysis/district-ranking")
    @Operation(summary = "Product Analysis district/ward ranking by settled tonnes (V2)",
            description = "When regionId and locationId are both omitted: ranks organization_regions (district) by settled tonnes descending. "
                    + "When either filter is present: ranks wards (organization_location) within scope — unchanged legacy behavior. "
                    + "Row bucket id/name use organizationLocationPublicId/organizationLocationName (region values in district mode).",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ProductAnalysisDistrictRankingV2DTO productAnalysisDistrictRankingV2(
            @RequestParam(name = "productId") UUID productId,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @RequestParam(name = "limit", required = false) Integer limit,
            @Parameter(hidden = true) Principal principal) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "product-analysis district-ranking");
        return supplierPortalProductAnalysisService.getDistrictRanking(
                productId, regionIds(queryParams), locationIds(queryParams), range.startInclusive(), range.endInclusive(), limit, principal);
    }

    @GetMapping("/product-analysis/district-cogs")
    @Operation(summary = "Product Analysis district/ward COGS ranking (V2)",
            description = "When regionId and locationId are both omitted: ranks organization_regions (district) by portal COGS descending. "
                    + "When either filter is present: ranks wards (organization_location) within scope — unchanged legacy behavior. "
                    + "Returns bucket slices plus totalCogsTzs (full-window product COGS for donut center). "
                    + "Row bucket id/name use organizationLocationPublicId/organizationLocationName (region values in district mode). "
                    + "COGS = Σ(product.buying_price × quantity_delivered).",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ProductAnalysisDistrictCogsV2DTO productAnalysisDistrictCogsV2(
            @RequestParam(name = "productId") UUID productId,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @RequestParam(name = "limit", required = false) Integer limit,
            @Parameter(hidden = true) Principal principal) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "product-analysis district-cogs");
        return supplierPortalProductAnalysisService.getDistrictCogs(
                productId, regionIds(queryParams), locationIds(queryParams), range.startInclusive(), range.endInclusive(), limit, principal);
    }

    @GetMapping("/product-analysis/ward-performance")
    @Operation(summary = "Product Analysis ward performance table (V2)",
            description = "Paged delivered aggregates for one product grouped by customer.organization_location. "
                    + "Fixed order: settledTonnes descending, organizationLocationName ascending (sort query params are rejected). "
                    + "Each row includes rank (global, page-offset aware), averageOrderValueTzs, and periodTrends. "
                    + "periodTrends.totalCogsTzsTrendPercentage and periodTrends.settledTonnesTrendPercentage vs previous comparable window. "
                    + "Distinct customer and order counts use COUNT(DISTINCT ...) to avoid join inflation.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ProductAnalysisWardPerformanceRowDTO> productAnalysisWardPerformanceV2(
            @RequestParam(name = "productId") UUID productId,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "product-analysis ward-performance");
        return supplierPortalProductAnalysisService.getWardPerformance(
                productId, regionIds(queryParams), locationIds(queryParams), range.startInclusive(), range.endInclusive(), principal, pageable);
    }

    @GetMapping("/product-analysis/sales-calendar")
    @Operation(summary = "Product Analysis sales calendar (V2)",
            description = "Daily portal COGS heat-map cells for one product in the requested calendar month (zero-filled), "
                    + "plus month summary cards: peakDay, totalOrders, totalQuantitySoldTonnes, totalCogsTzs. "
                    + "Uses explicit month (1–12) and year — no startDate/endDate. "
                    + "COGS = Σ(product.buying_price × quantity_delivered). Optional regionId/locationId filter customer's ward geography.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ProductAnalysisSalesCalendarV2DTO productAnalysisSalesCalendarV2(
            @RequestParam(name = "productId") UUID productId,
            @RequestParam(name = "month") Integer month,
            @RequestParam(name = "year") Integer year,
            @RequestParam MultiValueMap<String, String> queryParams,
            @Parameter(hidden = true) Principal principal) {
        return supplierPortalProductAnalysisService.getSalesCalendar(
                productId, regionIds(queryParams), locationIds(queryParams), month, year, principal);
    }

    @GetMapping("/product-analysis/all-products/summary-metrics")
    @Operation(summary = "Product Analysis summary cards — all supplier products (V2)",
            description = "Supplier-wide summary metrics aggregated across all products belonging to the authenticated supplier. "
                    + "No productId — use single-product /product-analysis/summary-metrics for drilldown. "
                    + "activeCustomers = COUNT(DISTINCT customers) with at least one DELIVERED supplier line "
                    + "(quantity_delivered > 0) — matches Dashboard summary-metrics activeCustomers and "
                    + "Customer Analysis summary-metrics customers for the same filters. "
                    + "Cards: activeCustomers (Customers), totalQuantitySoldTonnes (Total Units Sold), totalCogsTzs (COGS), "
                    + "averageOrderValueTzs (AOV = portal COGS ÷ delivered orders). totalOrders is retained for compatibility. "
                    + "COGS uses buying_price × quantity_delivered. "
                    + "Quantity uses settled tonnes (product.weight × quantity_delivered). "
                    + "Ward/district widgets group by customer.organization_location; regionId filters only.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ProductAnalysisSummaryMetricsV2DTO productAnalysisAllProductsSummaryMetricsV2(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @Parameter(hidden = true) Principal principal) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "product-analysis all-products summary-metrics");
        return supplierPortalProductAnalysisService.getAllProductsSummaryMetrics(
                regionIds(queryParams), locationIds(queryParams), range.startInclusive(), range.endInclusive(), principal);
    }

    @GetMapping("/product-analysis/all-products/sales-trend")
    @Operation(summary = "Product Analysis sales trend — all supplier products (V2)",
            description = "Ignores startDate/endDate. Mode A: month + year → daily series (zero-filled) with totalCogsTzs, orderCount, customerCount; headline = month COGS vs previous calendar month. "
                    + "Mode B: year only → 12 monthly points; headline = year COGS vs prior calendar year. month without year is invalid. "
                    + "No productId — use /product-analysis/sales-trend for single-product drilldown.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ProductAnalysisSalesTrendV2DTO productAnalysisAllProductsSalesTrendV2(
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam MultiValueMap<String, String> queryParams,
            @Parameter(hidden = true) Principal principal) {
        return supplierPortalProductAnalysisService.getAllProductsSalesTrend(
                regionIds(queryParams), locationIds(queryParams), month, year, principal);
    }

    @GetMapping("/product-analysis/all-products/district-ranking")
    @Operation(summary = "Product Analysis district/ward ranking — all supplier products (V2)",
            description = "Product Sales Distribution table feed. No regionId and no locationId: ranks districts (organization_regions) "
                    + "by settled tonnes (Units Sold). Either filter present: ranks wards within scope (unchanged). "
                    + "Each row includes rank, settledTonnes, averageOrderValueTzs, periodTrends vs previous comparable window. "
                    + "totalSettledTonnes is the full-window aggregate (not row-limited). "
                    + "No productId — use /product-analysis/district-ranking for single-product drilldown.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ProductAnalysisDistrictRankingV2DTO productAnalysisAllProductsDistrictRankingV2(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @RequestParam(name = "limit", required = false) Integer limit,
            @Parameter(hidden = true) Principal principal) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "product-analysis all-products district-ranking");
        return supplierPortalProductAnalysisService.getAllProductsDistrictRanking(
                regionIds(queryParams), locationIds(queryParams), range.startInclusive(), range.endInclusive(), limit, principal);
    }

    @GetMapping("/product-analysis/all-products/district-cogs")
    @Operation(summary = "Product Analysis district/ward COGS ranking — all supplier products (V2)",
            description = "No regionId and no locationId: ranks districts (organization_regions) by portal COGS descending. "
                    + "Either filter present: ranks wards within scope (unchanged). Each row includes rank, totalCogsTzs, "
                    + "and periodTrends.totalCogsTzsTrendPercentage vs previous comparable window. "
                    + "totalCogsTzs is full-window supplier-wide COGS (independent of row limit). "
                    + "For product-level COGS ranking use /product-analysis/all-products/product-ranking-cogs.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ProductAnalysisDistrictCogsV2DTO productAnalysisAllProductsDistrictCogsV2(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @RequestParam(name = "limit", required = false) Integer limit,
            @Parameter(hidden = true) Principal principal) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "product-analysis all-products district-cogs");
        return supplierPortalProductAnalysisService.getAllProductsDistrictCogs(
                regionIds(queryParams), locationIds(queryParams), range.startInclusive(), range.endInclusive(), limit, principal);
    }

    @GetMapping("/product-analysis/all-products/product-ranking-cogs")
    @Operation(summary = "Product Analysis product COGS ranking — all supplier products (V2)",
            description = "Top 10 supplier-wide product rows by portal COGS descending (fixed order; page/size query params ignored; sort rejected). "
                    + "Full ranking available via product-analysis export (ALL_PRODUCTS_PRODUCT_RANKING_COGS). "
                    + "Each row is product-scoped — not ward/location. Use /product-analysis/all-products/district-cogs for district/ward COGS slices. "
                    + "COGS = Σ(product.buying_price × quantity_delivered). totalCogsTzs remains numeric (BigDecimal) for sorting/math; "
                    + "bind totalCogsTzsLabel for display (comma-formatted, e.g. TZS 1,250,000). "
                    + "totalQuantitySoldUnits = total units sold (Σ quantity_delivered). "
                    + "unitOfMeasure = products.unit_of_measure (e.g. Bag, Carton). "
                    + "Each row includes periodTrends.totalCogsTzsTrendPercentage vs the immediately preceding window of equal inclusive day count. "
                    + "Optional regionId/locationId filter customer's ward geography.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ProductAnalysisProductRankingCogsRowV2DTO> productAnalysisAllProductsProductRankingCogsV2(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "product-analysis all-products product-ranking-cogs");
        return supplierPortalProductAnalysisService.getAllProductsProductRankingCogs(
                regionIds(queryParams), locationIds(queryParams), range.startInclusive(), range.endInclusive(), principal, pageable);
    }

    @GetMapping("/product-analysis/all-products/ward-performance")
    @Operation(summary = "Product Analysis ward performance table — all supplier products (V2)",
            description = "Paged delivered aggregates across all supplier products grouped by customer.organization_location. "
                    + "No productId — use /product-analysis/ward-performance for single-product drilldown. "
                    + "Fixed order: settledTonnes descending, organizationLocationName ascending (sort query params are rejected). "
                    + "Each row includes rank (global, page-offset aware), averageOrderValueTzs, and periodTrends. "
                    + "periodTrends.totalCogsTzsTrendPercentage and periodTrends.settledTonnesTrendPercentage vs previous comparable window. "
                    + "Distinct customer and order counts use COUNT(DISTINCT ...) to avoid join inflation.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ProductAnalysisWardPerformanceRowDTO> productAnalysisAllProductsWardPerformanceV2(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "product-analysis all-products ward-performance");
        return supplierPortalProductAnalysisService.getAllProductsWardPerformance(
                regionIds(queryParams), locationIds(queryParams), range.startInclusive(), range.endInclusive(), principal, pageable);
    }

    @GetMapping("/product-analysis/all-products/sales-calendar")
    @Operation(summary = "Product Analysis sales calendar — all supplier products (V2)",
            description = "Daily portal COGS heat-map cells across all supplier products for the requested calendar month (zero-filled), "
                    + "plus month summary cards: peakDay, totalOrders, totalQuantitySoldTonnes, totalCogsTzs. "
                    + "No productId — use /product-analysis/sales-calendar for single-product drilldown. "
                    + "Uses explicit month (1–12) and year — no startDate/endDate. "
                    + "COGS = Σ(product.buying_price × quantity_delivered). Optional regionId/locationId filter customer's ward geography.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ProductAnalysisSalesCalendarV2DTO productAnalysisAllProductsSalesCalendarV2(
            @RequestParam(name = "month") Integer month,
            @RequestParam(name = "year") Integer year,
            @RequestParam MultiValueMap<String, String> queryParams,
            @Parameter(hidden = true) Principal principal) {
        return supplierPortalProductAnalysisService.getAllProductsSalesCalendar(
                regionIds(queryParams), locationIds(queryParams), month, year, principal);
    }

    @GetMapping("/customer-analysis/summary-metrics")
    @Operation(summary = "Customer Analysis summary cards (V2)",
            description = "Customer-scoped summary KPIs with prior-window trends matching Dashboard V2 semantics "
                    + "(label 'vs Previous Period'; trendPercentage never null on summary cards). "
                    + "customers = COUNT(DISTINCT customers) with at least one DELIVERED supplier line "
                    + "(quantity_delivered > 0) — matches Dashboard activeCustomers and all-products activeCustomers "
                    + "for the same date + geography filters; "
                    + "newCustomers = customers whose earliest delivered supplier-portal order "
                    + "(MIN(so.creation_time) across qualifying DELIVERED lines in the filtered geography) falls in the window; "
                    + "aov = portal COGS (Σ(product.buying_price × quantity_delivered)) / delivered order count; "
                    + "frequentCustomers = distinct customers with at least two delivered supplier orders "
                    + "(COUNT(DISTINCT so.public_id) >= 2) in the filtered window. "
                    + "Ward/district widgets group by customer.organization_location; regionId filters only.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public CustomerAnalysisSummaryMetricsV2DTO customerAnalysisSummaryMetricsV2(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @Parameter(hidden = true) Principal principal) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "customer-analysis summary-metrics");
        return supplierPortalCustomerAnalysisService.getSummaryMetrics(
                regionIds(queryParams), locationIds(queryParams), productIds(queryParams),
                range.startInclusive(), range.endInclusive(), principal);
    }

    @GetMapping("/customer-analysis/customers")
    @Operation(summary = "Customer Analysis paged customer table (V2)",
            description = "Paged delivered-order aggregates per customer for the authenticated supplier. "
                    + "Fixed order: totalCogsTzs descending, shopName ascending — sort query params are rejected (400). "
                    + "Page size 1–100. "
                    + "COGS uses buying_price × quantity_delivered. "
                    + "Optional search: case-insensitive substring match on shop name (store_name) only (trimmed; blank omitted).",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<CustomerAnalysisCustomerRowV2DTO> customerAnalysisCustomersV2(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @RequestParam(name = "search", required = false) String search,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "customer-analysis customers");
        return supplierPortalCustomerAnalysisService.getCustomers(
                regionIds(queryParams),
                locationIds(queryParams),
                productIds(queryParams),
                range.startInclusive(), range.endInclusive(), search, principal, pageable);
    }

    @GetMapping("/customer-analysis/district-ranking")
    @Operation(summary = "Customer Analysis district/ward ranking by distinct customers (V2)",
            description = "When regionId and locationId are both omitted: ranks organization_regions (district) by distinctCustomerCount DESC, "
                    + "tie-break organizationLocationName ascending (region name in district mode). "
                    + "When either filter is present: ranks wards (organization_location) within scope — unchanged. "
                    + "Preserves deliveredOrderCount and averageOrderValueTzs (portal COGS ÷ delivered orders). "
                    + "totalAverageOrderValueTzs is full-window AOV for the filtered scope (independent of row limit). "
                    + "Row bucket uses organizationLocationPublicId/organizationLocationName.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public CustomerAnalysisDistrictRankingV2DTO customerAnalysisDistrictRankingV2(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @RequestParam(name = "limit", required = false) Integer limit,
            @Parameter(hidden = true) Principal principal) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "customer-analysis district-ranking");
        return supplierPortalCustomerAnalysisService.getDistrictRanking(
                regionIds(queryParams), locationIds(queryParams), productIds(queryParams),
                range.startInclusive(), range.endInclusive(), limit, principal);
    }

    @GetMapping("/customer-analysis/district-cogs")
    @Operation(summary = "Customer Analysis district/ward COGS ranking (V2)",
            description = "When regionId and locationId are both omitted: ranks organization_regions (district) by portal COGS descending. "
                    + "When either filter is present: ranks wards (organization_location) within scope — unchanged legacy behavior. "
                    + "Separate widget contract from district-ranking (distinct-customer axis). "
                    + "Returns bucket slices plus totalCogsTzs (full-window supplier COGS for donut center, independent of row limit). "
                    + "Row bucket uses organizationLocationPublicId/organizationLocationName. "
                    + "COGS = Σ(product.buying_price × quantity_delivered).",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public CustomerAnalysisDistrictCogsV2DTO customerAnalysisDistrictCogsV2(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @RequestParam(name = "limit", required = false) Integer limit,
            @Parameter(hidden = true) Principal principal) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "customer-analysis district-cogs");
        return supplierPortalCustomerAnalysisService.getDistrictCogs(
                regionIds(queryParams), locationIds(queryParams), productIds(queryParams),
                range.startInclusive(), range.endInclusive(), limit, principal);
    }

    @GetMapping("/customer-analysis/orders-trend")
    @Operation(summary = "Customer Analysis delivered orders trend (V2)",
            description = "Ignores startDate/endDate. Mode A: month + year → daily distinct delivered-order counts (zero-filled); headline = month total vs previous calendar month. "
                    + "Mode B: year only → 12 monthly points; headline = year total vs prior calendar year. "
                    + "trendPercentage is null when the comparison baseline is zero.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public CustomerAnalysisOrdersTrendV2DTO customerAnalysisOrdersTrendV2(
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam MultiValueMap<String, String> queryParams,
            @Parameter(hidden = true) Principal principal) {
        return supplierPortalCustomerAnalysisService.getOrdersTrend(
                regionIds(queryParams), locationIds(queryParams), productIds(queryParams), month, year, principal);
    }

    @GetMapping("/customer-analysis/activity-calendar")
    @Operation(summary = "Customer Analysis activity calendar (V2)",
            description = "Calendar-month customer activity heat map. Requires explicit month (1–12) and year — no startDate/endDate. "
                    + "days[]: unchanged — one zero-filled cell per day with distinctCustomerCount, deliveredOrderCount, and heat intensity "
                    + "(normalized against the month peak distinct-customer day; bands NONE/LOW/MEDIUM/HIGH/PEAK). "
                    + "summary: peakDay (distinct-customer axis, earliest date on ties, no trend), "
                    + "totalOrders (TrendMetricDTO — distinct delivered orders in the month vs previous comparable window), "
                    + "unitsSold (TrendMetricDTO — settled quantity sold in the month: Σ(product.weight × quantity_delivered) "
                    + "on delivered supplier lines). "
                    + "Optional regionId/locationId/productId filter customer's ward geography and supplier products.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public CustomerAnalysisActivityCalendarV2DTO customerAnalysisActivityCalendarV2(
            @RequestParam(name = "month") Integer month,
            @RequestParam(name = "year") Integer year,
            @RequestParam MultiValueMap<String, String> queryParams,
            @Parameter(hidden = true) Principal principal) {
        return supplierPortalCustomerAnalysisService.getActivityCalendar(
                regionIds(queryParams), locationIds(queryParams), productIds(queryParams), month, year, principal);
    }

    @GetMapping("/customer-locations/markers")
    @Operation(summary = "Customer Locations map markers (V2)",
            description = "Unpaged distinct customers with privacy-preserving map pins for the authenticated supplier. "
                    + "latitude/longitude are geohash cell centers at precision 5 (~5 km) — the same hashing used by "
                    + "GET /customer-locations/heat-map — so exact customer coordinates never leave the API. "
                    + "All-time supplier-scoped delivered orders; no date range filter. "
                    + "Categories: NEW, ACTIVE, INACTIVE, CHURNED (churn when last supplier delivery >30 days ago). "
                    + "Invalid/unparseable coordinates are omitted server-side. Soft cap maxMarkers=5000 with meta. "
                    + "Filter dropdowns reuse GET /authentication/regions, GET /authentication/locations, and "
                    + "GET /products/supplier/{supplierPublicId}.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public CustomerLocationsMarkersV2DTO customerLocationsMarkersV2(
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "productId", required = false) UUID productId,
            @Parameter(hidden = true) Principal principal) {
        return supplierPortalCustomerLocationsService.getMarkers(
                regionId, locationId, productId, principal);
    }

    @GetMapping("/customer-locations/heat-map")
    @Operation(summary = "Customer Locations heat map cells (V2)",
            description = "Privacy-preserving heat map: per-category geohash cell centers with customer counts "
                    + "(NEW, ACTIVE, CHURNED) for the authenticated supplier. INACTIVE customers are omitted. "
                    + "Optional category=NEW|ACTIVE|CHURNED returns cells for that tab only "
                    + "(other category keys remain present but empty); omit category to return all three. "
                    + "Exact customer coordinates "
                    + "are never returned — points are aggregated at geohash precision 5 (~5 km cells). "
                    + "All-time supplier-scoped delivered orders; no date range filter. CHURNED when the "
                    + "customer's last-ever delivered order from this supplier is more than 30 days ago. "
                    + "Invalid coordinates are omitted server-side. Soft input cap maxPoints=5000 before aggregation "
                    + "(meta.truncated). Filter dropdowns reuse GET /authentication/regions, "
                    + "GET /authentication/locations, and GET /products/supplier/{supplierPublicId}.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public CustomerLocationsHeatMapV2DTO customerLocationsHeatMapV2(
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "category", required = false) CustomerLocationMarkerCategoryV2 category,
            @Parameter(hidden = true) Principal principal) {
        return supplierPortalCustomerLocationsService.getHeatMap(
                regionId, locationId, productId, category, principal);
    }

    @GetMapping("/customer-locations/customer/{customerId}")
    @Operation(summary = "Customer Locations selected customer detail (V2)",
            description = "Sole marker-click detail source for Customer Locations V2. Rich customer detail for "
                    + "supplier-scoped customers. Base fields always return when in scope; periodMetrics is included "
                    + "only when both startDate and endDate are provided (one-sided dates return 400). "
                    + "Out-of-scope customers return 404. Phone numbers are masked; use phoneVisible for display gating. "
                    + "Optional regionId, locationId, and productId use the same filter sources as markers "
                    + "(GET /authentication/regions, GET /authentication/locations, GET /products/supplier/{supplierPublicId}).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Customer not in supplier scope", content = @Content)
            })
    public CustomerLocationDetailV2DTO customerLocationDetailV2(
            @PathVariable UUID customerId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal) {
        return supplierPortalCustomerLocationsService.getCustomerDetail(
                customerId, regionId, locationId, productId, startDate, endDate, principal);
    }

    @JsonView(BaseView.OrganizationLocationsView.class)
    @GetMapping("/organization-locations")
    @Operation(summary = "Supplier Portal ward lookup (V2)",
            description = "Wards (organization locations) for supplier portal filter dropdowns. "
                    + "Accepts one or many organizationRegionId values (district public ids). "
                    + "Returns a scrollable dropdown list — clients typically request page=0&size=500 in one call. "
                    + "Legacy GET /organizationLocations remains single-district only.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    @PageableAsQueryParam
    public Page<OrganizationLocations> supplierPortalOrganizationLocations(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam MultiValueMap<String, String> queryParams,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        List<UUID> organizationRegionIds = SupplierPortalGeographyQueryParams.parseUuidList(
                queryParams, "organizationRegionId");
        return supplierPortalOrganizationLocationsService.getOrganizationLocations(
                searchParam, name, organizationRegionIds, pageable);
    }

    @GetMapping("/dashboard/product-distribution-trend")
    @Operation(summary = "Supplier product distribution report", description = "Same as GET /reports/supplierProductDistributionReport",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {
                            @ExampleObject(name = "GET_STATISTICS_product", value = Examples.FETCH_PRODUCT_DISTRIBUTION)})),
            })
    public Page<ProductSaleDistributionDTO> supplierProductDistributionTrend(
            @RequestParam(name = "region", required = false) UUID regionPublicId,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.generateSupplierDistributionReport(regionPublicId, start, end, principal.getName(), pageable);
    }

    @GetMapping("/dashboard/customer-performance")
    @Operation(summary = "Product customer performance report", description = "Same as GET /reports/productCustomerPerformanceReport",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ProductCustomerPerformanceDTO> supplierProductCustomerPerformance(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        return reportService.getProductCustomerPerformanceReport(productId,
                regionId,
                locationId,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    @GetMapping("/dashboard/order-value-distribution")
    @Operation(summary = "Order value per zone", description = "Same as GET /reports/orderValuePerZone",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ZoneNameAverageProjection> supplierOrderValuePerZone(
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "productId", required = false) UUID productId,
            @PageableDefault(size = 1000, sort = "creationTime",
                    direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal) {
        return salesOrdersService.orderValuePerZone(productId, regionId, startDate, endDate, locationId, pageable,
                principal);
    }

    @GetMapping("/dashboard/product-performance-trend")
    @Operation(summary = "Product performance report", description = "Same as GET /reports/productPerformanceReport",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<ProductPerformanceDTO>> supplierProductPerformanceTrend(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "zones", required = false) List<UUID> zones,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        return reportService.getProductPerformanceReport(productId,
                regionId,
                zones,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    @GetMapping("/dashboard/product-performance-zone")
    @Operation(summary = "Product performance per zone report", description = "Same as GET /reports/productPerformancePerZoneReport",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<ProductPerformancePerZone>> supplierProductPerformancePerZone(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "zones", required = false) List<UUID> zones,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        return reportService.getProductPerformancePerZoneReport(productId,
                regionId,
                zones,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    @GetMapping("/dashboard/product-performance-date-range")
    @Operation(summary = "Product performance by zone (date range)", description = "Same as GET /reports/productPerformanceDateRangeByZone",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<ZoneAverageDTO1> supplierProductPerformanceDateRangeByZone(
            @RequestParam(name = "locationId", required = false) UUID zoneId,
            @RequestParam(name = "productId", required = false) UUID productId,
            @PageableDefault(size = 1000, sort = "creationTime",
                    direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal) {
        return salesOrdersService.productPerformanceDateRangeByZone(productId, zoneId, startDate, endDate, pageable, principal);
    }

    @GetMapping("/dashboard/sales-purchases")
    @Operation(summary = "Supplier purchase report", description = "Same as GET /reports/purchaseReport",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<CustomerOrderDTORapper> supplierSalesPurchases(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 1000, direction = Sort.Direction.ASC) Pageable pageable) {
        return reportService.getPurchaseReport(searchParam, regionId, startDate, endDate, principal.getName(), pageable);
    }

    @GetMapping("/products/performance-per-zone")
    @Operation(summary = "Product performance per zone report", description = "Same as GET /reports/productPerformancePerZoneReport",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<ProductPerformancePerZone>> productsPerformancePerZone(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "zones", required = false) List<UUID> zones,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        return reportService.getProductPerformancePerZoneReport(productId,
                regionId,
                zones,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    @GetMapping("/products/date-range-by-zone")
    @Operation(summary = "Product distribution by zone (date range)", description = "Same as GET /reports/productPerformanceDateRangeByZone",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<ZoneAverageDTO1> productsDateRangeByZone(
            @RequestParam(name = "locationId", required = false) UUID zoneId,
            @RequestParam(name = "productId", required = false) UUID productId,
            @PageableDefault(size = 1000, sort = "creationTime",
                    direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal) {
        return salesOrdersService.productPerformanceDateRangeByZone(productId, zoneId, startDate, endDate, pageable, principal);
    }

    @GetMapping("/products/order-value-per-zone")
    @Operation(summary = "Order value per zone", description = "Same as GET /reports/orderValuePerZone",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ZoneNameAverageProjection> productsOrderValuePerZone(
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "productId", required = false) UUID productId,
            @PageableDefault(size = 1000, sort = "creationTime",
                    direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal) {
        return salesOrdersService.orderValuePerZone(productId, regionId, startDate, endDate, locationId, pageable,
                principal);
    }

    @GetMapping("/products/performance-timeline")
    @Operation(summary = "Product performance (timeline)", description = "Same as GET /reports/productPerformance",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<ZoneNameDateAverageProjection> productsPerformanceTimeline(
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "productId", required = false) UUID productId,
            @PageableDefault(size = 1000, sort = "creationTime",
                    direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal) {
        return salesOrdersService.productPerformance(productId, regionId, startDate, endDate, locationId, pageable,
                principal);
    }

    @GetMapping("/products/performance-monthly")
    @Operation(summary = "Product performance report (monthly / area chart)", description = "Same as GET /reports/productPerformanceReport",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<ProductPerformanceDTO>> productsPerformanceMonthly(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "zones", required = false) List<UUID> zones,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        return reportService.getProductPerformanceReport(productId,
                regionId,
                zones,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    @GetMapping("/products/customer-aov")
    @Operation(summary = "Product customer performance report", description = "Same as GET /reports/productCustomerPerformanceReport",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ProductCustomerPerformanceDTO> productsCustomerAov(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        return reportService.getProductCustomerPerformanceReport(productId,
                regionId,
                locationId,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    @GetMapping("/products/export-customer-performance")
    @Operation(summary = "Customer performance export (Excel)", description = "Same as GET /reports/productCustomerPerformanceExport",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> exportProductsCustomerPerformance(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "zones", required = false) UUID locationId,
            @RequestParam(name = "startDate", required = false) String starDate,
            @RequestParam(name = "endDate", required = false) String enDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate startDate = DateFormatter.parseDate(starDate);
        LocalDate endDate = DateFormatter.parseDate(enDate);
        ByteArrayOutputStream byteArrayOutputStream = reportService.productCustomerPerformanceExport(productId,
                regionId,
                locationId,
                startDate,
                endDate,
                principal.getName(),
                StatementExportType.EXCEL,
                principal.getName(), pageable);
        String contentDisposition = String.format("attachment; filename=\"Customer Performance Report - %s.%s\"",
                LocalDateTime.now().toString(),
                StatementExportType.EXCEL);
        String mediaType = StatementExportType.EXCEL.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/sales")
    @Operation(summary = "Supplier purchase / sales report", description = "Same as GET /reports/purchaseReport",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<CustomerOrderDTORapper> supplierSales(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 1000, direction = Sort.Direction.ASC) Pageable pageable) {
        return reportService.getPurchaseReport(searchParam, regionId, startDate, endDate, principal.getName(), pageable);
    }

    @GetMapping("/sales/export")
    @Operation(summary = "Supplier purchase / sales report export", description = "Same as GET /reports/purchaseReport/export",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<?> supplierSalesExport(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 1000, direction = Sort.Direction.ASC) Pageable pageable) {
        Page<CustomerOrderDTORapper> customerOrderPageable = reportService.getPurchaseReport(searchParam, regionId, startDate,
                endDate,
                principal.getName(), pageable);
        ByteArrayOutputStream byteArrayOutputStream = reportService.generatePurchaseOrderReportExport(customerOrderPageable.getContent(),
                exportType,
                principal.getName());
        String contentDisposition = String.format("attachment; filename=\"purchase Report - %s.%s\"",
                LocalDateTime.now().toString(),
                exportType.extension);
        String mediaType = exportType.contentType;
        return ResponseEntity.ok().contentLength(byteArrayOutputStream.size())
                .contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", contentDisposition)
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/distribution/total-orders")
    @Operation(summary = "Supplier total orders", description = "Same as GET /reports/supplierTotalOrders",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {
                    @ExampleObject(name = "GET_Distribution", value = Examples.FETCH_PRODUCT_DISTRIBUTION_PER_REGION)}))})
    public Map<String, Integer> distributionTotalOrders(
            @RequestParam(name = "regionId", required = false) UUID regionPublicId,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @Parameter(hidden = true) Principal principal) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.generateSupplierTotalOrdersReport(regionPublicId, start, end, principal.getName());
    }

    @GetMapping("/distribution/quantity-sold")
    @Operation(summary = "Supplier total quantity sold", description = "Same as GET /reports/supplierTotalQuantitySold",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {
                    @ExampleObject(name = "GET_Distribution", value = Examples.FETCH_PRODUCT_DISTRIBUTION_PER_REGION)}))})
    public Map<String, Integer> distributionQuantitySold(
            @RequestParam(name = "regionId", required = false) UUID regionPublicId,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @Parameter(hidden = true) Principal principal) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.getQuantitySoldReport(regionPublicId, start, end, principal.getName());
    }

    @GetMapping("/distribution/active-customers")
    @Operation(summary = "Total active customers", description = "Same as GET /reports/totalActiveCustomers",
            responses = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {
                    @ExampleObject(name = "GET_Distribution", value = Examples.FETCH_PRODUCT_DISTRIBUTION_PER_REGION)}))})
    public Map<String, Integer> distributionActiveCustomers(
            @RequestParam(name = "regionId", required = false) UUID regionPublicId,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @Parameter(hidden = true) Principal principal) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.getTotalActiveCustomers(regionPublicId, start, end, principal.getName());
    }

    @GetMapping("/distribution/products")
    @Operation(summary = "Supplier product distribution", description = "Same as GET /reports/supplierProductDistributionReport",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {
                            @ExampleObject(name = "GET_STATISTICS_product", value = Examples.FETCH_PRODUCT_DISTRIBUTION)})),
            })
    public Page<ProductSaleDistributionDTO> distributionProducts(
            @RequestParam(name = "region", required = false) UUID regionPublicId,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.generateSupplierDistributionReport(regionPublicId, start, end, principal.getName(), pageable);
    }

    @GetMapping("/distribution/quantity-by-region")
    @Operation(summary = "Quantity sold per region", description = "Same as GET /reports/supplierQuantitySoldPerRegion",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {
                            @ExampleObject(name = "GET_Distribution", value = Examples.FETCH_PRODUCT_DISTRIBUTION_PER_REGION)})),
            })
    public ResponseEntity<List<QuantitySoldPerRegionDTO>> distributionQuantityByRegion(
            @RequestParam(name = "region", required = false) UUID regionPublicId,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate start = DateFormatter.parseDate(startDate);
        LocalDate end = DateFormatter.parseDate(endDate);
        return reportService.generateSupplierProductDistributionReport(regionPublicId, start, end, principal.getName(), pageable);
    }

    @GetMapping("/product-analysis/export")
    @Operation(summary = "Product Analysis V2 export (Excel / PDF)",
            description = "Exports the requested product analysis view as XLSX or PDF. "
                    + "Pass the same filter params as the corresponding GET (productId, regionId, locationId, startDate, endDate, limit). "
                    + "productId is required for WARD_PERFORMANCE and DISTRICT_RANKING/DISTRICT_COGS single-product views. "
                    + "ALL_PRODUCTS_* views ignore productId. "
                    + "Paged views (WARD_PERFORMANCE, ALL_PRODUCTS_WARD_PERFORMANCE, ALL_PRODUCTS_PRODUCT_RANKING_COGS) "
                    + "are capped at " + SupplierPortalAnalysisExportHelper.EXPORT_MAX + " rows. "
                    + "Filename format: {SupplierName}_{ViewTitle}_{yyyyMMdd_HHmmss}.{ext}.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(examples = {
                                    @ExampleObject(name = "SUPPLIER_PORTAL_PRODUCT_ANALYSIS_EXPORT",
                                            value = Examples.SUPPLIER_PORTAL_PRODUCT_ANALYSIS_EXPORT)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ResponseEntity<?> exportProductAnalysis(
            @RequestParam(name = "view") SupplierPortalProductAnalysisView view,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @RequestParam(name = "limit", required = false) Integer limit,
            @Parameter(hidden = true) Principal principal) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "product-analysis export");
        Suppliers supplier = analysisExportHelper.resolveSupplier(principal);
        ByteArrayOutputStream bos = analysisExportHelper.exportProductAnalysis(
                view, exportType, productId, regionIds(queryParams), locationIds(queryParams),
                range.startInclusive(), range.endInclusive(), limit, supplier, principal);
        String filename = analysisExportHelper.exportFilename(supplier.getSupplierName(), view.displayName, exportType);
        return ResponseEntity.ok()
                .contentLength(bos.size())
                .contentType(MediaType.parseMediaType(exportType.contentType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(bos.toByteArray());
    }

    @GetMapping("/customer-analysis/export")
    @Operation(summary = "Customer Analysis V2 export (Excel / PDF)",
            description = "Exports the requested customer analysis view as XLSX or PDF. "
                    + "Pass the same filter params as the corresponding GET (regionId, locationId, startDate, endDate, limit, search). "
                    + "CUSTOMERS view exports every matching delivered order for the filter (uncapped; one row per order). "
                    + "Filename format: {SupplierName}_{ViewTitle}_{yyyyMMdd_HHmmss}.{ext}.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(examples = {
                                    @ExampleObject(name = "SUPPLIER_PORTAL_CUSTOMER_ANALYSIS_EXPORT",
                                            value = Examples.SUPPLIER_PORTAL_CUSTOMER_ANALYSIS_EXPORT)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ResponseEntity<?> exportCustomerAnalysis(
            @RequestParam(name = "view") SupplierPortalCustomerAnalysisView view,
            @RequestParam(name = "exportType") StatementExportType exportType,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "search", required = false) String search,
            @Parameter(hidden = true) Principal principal) {
        var range = resolvePortalDateRange(startDate, endDate, year, month, quarter, "customer-analysis export");
        Suppliers supplier = analysisExportHelper.resolveSupplier(principal);
        ByteArrayOutputStream bos = analysisExportHelper.exportCustomerAnalysis(
                view, exportType, regionId, locationId, productIds(queryParams),
                range.startInclusive(), range.endInclusive(), limit, search, supplier, principal);
        String filename = analysisExportHelper.exportFilename(supplier.getSupplierName(), view.displayName, exportType);
        return ResponseEntity.ok()
                .contentLength(bos.size())
                .contentType(MediaType.parseMediaType(exportType.contentType))
                .cacheControl(CacheControl.noCache())
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(bos.toByteArray());
    }

    @GetMapping("/distribution/revenue-trend")
    @Operation(summary = "Supplier sales trend (V2, distribution alias)",
            description = "Alias of /dashboard/revenue-trend — same month/year two-mode contract (daily-in-month or monthly-in-year). "
                    + "month without year is invalid; monthly/quarterly/yearly flags return 400.",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public SalesTrendV2DTO distributionRevenueTrend(
            @RequestParam(name = "region", required = false) UUID regionPublicId,
            @RequestParam(name = "location", required = false) UUID locationPublicId,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "monthly", required = false) Boolean monthly,
            @RequestParam(name = "quarterly", required = false) Boolean quarterly,
            @RequestParam(name = "yearly", required = false) Boolean yearly,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 100, direction = Sort.Direction.ASC) Pageable pageable) {
        return supplierPortalDashboardService.getSalesTrend(
                optionalUuidList(regionPublicId),
                optionalUuidList(locationPublicId),
                month,
                year,
                monthly,
                quarterly,
                yearly,
                principal,
                pageable);
    }

    private List<UUID> regionIds(MultiValueMap<String, String> queryParams) {
        return SupplierPortalGeographyQueryParams.parseUuidList(queryParams, "regionId");
    }

    private List<UUID> locationIds(MultiValueMap<String, String> queryParams) {
        return SupplierPortalGeographyQueryParams.parseUuidList(queryParams, "locationId");
    }

    private List<UUID> productIds(MultiValueMap<String, String> queryParams) {
        return SupplierPortalGeographyQueryParams.parseUuidList(queryParams, "productId");
    }

    private List<UUID> optionalUuidList(UUID id) {
        return id == null ? List.of() : List.of(id);
    }

    private UUID firstUuid(MultiValueMap<String, String> queryParams, String primaryParamName, String fallbackParamName) {
        List<UUID> values = SupplierPortalGeographyQueryParams.parseUuidList(queryParams, primaryParamName);
        if (values.isEmpty()) {
            values = SupplierPortalGeographyQueryParams.parseUuidList(queryParams, fallbackParamName);
        }
        return values.isEmpty() ? null : values.get(0);
    }

    private SupplierPortalDateRangeFilter.ResolvedDateRange resolvePortalDateRange(LocalDate startDate,
                                                                                    LocalDate endDate,
                                                                                    Integer year,
                                                                                    Integer month,
                                                                                    Integer quarter,
                                                                                    String context) {
        return SupplierPortalDateRangeFilter.resolve(
                startDate, endDate, year, month, quarter, LocalDate.now(EAST_AFRICA), context);
    }
}
