package com.app.itibari.services;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.report.ProductAnalysisDailyCogsPointDTO;
import com.app.itibari.models.report.ProductAnalysisDistrictCogsRowDTO;
import com.app.itibari.models.report.ProductAnalysisDistrictCogsV2DTO;
import com.app.itibari.models.report.ProductAnalysisDistrictRankingRowDTO;
import com.app.itibari.models.report.ProductAnalysisDistrictRankingV2DTO;
import com.app.itibari.models.report.ProductAnalysisProductRankingCogsRowV2DTO;
import com.app.itibari.models.report.ProductAnalysisSalesCalendarPeakDayV2DTO;
import com.app.itibari.models.report.ProductAnalysisSalesCalendarV2DTO;
import com.app.itibari.models.report.ProductAnalysisSalesTrendV2DTO;
import com.app.itibari.models.report.ProductAnalysisSummaryMetricsV2DTO;
import com.app.itibari.models.report.ProductAnalysisWardPeriodTrendsDTO;
import com.app.itibari.models.report.ProductAnalysisWardPerformanceRowDTO;
import com.app.itibari.models.report.TrendMetricDTO;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.services.helper.ProductImageResolver;
import com.app.itibari.services.supplierportal.SupplierPortalDashboardMath;
import com.app.itibari.services.supplierportal.SupplierPortalGeographyFilter;
import com.app.itibari.services.supplierportal.SupplierPortalProductFilter;
import com.app.itibari.services.supplierportal.SupplierPortalProductAnalysisValidation;
import com.app.itibari.services.supplierportal.SupplierPortalTrendGranularity;
import com.app.itibari.services.supplierportal.SupplierPortalTrendPeriod;
import com.app.itibari.services.supplierportal.SupplierPortalTrendPeriodValidation;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Supplier Portal Product Analysis V2 — product-scoped analytics with portal COGS and ward grouping
 * ({@code customers.organization_location}). Intentionally bypasses legacy {@link ReportService} math.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierPortalProductAnalysisServiceImpl implements SupplierPortalProductAnalysisService {

    private static final ZoneId EAST_AFRICA = ZoneId.of("Africa/Dar_es_Salaam");
    private static final String LABEL_PREVIOUS_PERIOD = "vs Previous Period";
    private static final String LABEL_VS_PREVIOUS_MONTH = "vs Previous Month";
    private static final String LABEL_VS_LAST_YEAR = "vs Last Year";
    private static final int DEFAULT_DISTRICT_RANKING_LIMIT = 10;

    private final SalesOrdersRepository salesOrdersRepository;
    private final PersonRepository personRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ProductImageResolver productImageResolver;

    @Override
    public ProductAnalysisSummaryMetricsV2DTO getSummaryMetrics(UUID productId,
                                                              List<UUID> regionIds,
                                                              List<UUID> locationIds,
                                                              LocalDate startDate,
                                                              LocalDate endDate,
                                                              Principal principal) {
        SupplierPortalProductAnalysisValidation.validateDateRange(startDate, endDate);
        Suppliers supplier = resolveSupplier(principal);
        assertProductOwnedBySupplier(productId, supplier);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        LocalDateTime currentStart = dateStartInclusive(startDate);
        LocalDateTime currentEnd = dateEndInclusive(endDate);
        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousStart = dateStartInclusive(prev[0]);
        LocalDateTime previousEnd = dateEndInclusive(prev[1]);

        UUID supplierId = supplier.getPublicId();
        long activeCustomersCurrent = nz(countProductActiveCustomers(supplierId, productId, geo, currentStart, currentEnd));
        long activeCustomersPrevious = nz(countProductActiveCustomers(supplierId, productId, geo, previousStart, previousEnd));

        long ordersCurrent = nz(countProductDeliveredOrders(supplierId, productId, geo, currentStart, currentEnd));
        long ordersPrevious = nz(countProductDeliveredOrders(supplierId, productId, geo, previousStart, previousEnd));

        BigDecimal tonnesCurrent = nzBd(sumProductSettledTonnes(supplierId, productId, geo, currentStart, currentEnd));
        BigDecimal tonnesPrevious = nzBd(sumProductSettledTonnes(supplierId, productId, geo, previousStart, previousEnd));

        BigDecimal cogsCurrent = nzBd(sumProductCogs(supplierId, productId, geo, currentStart, currentEnd));
        BigDecimal cogsPrevious = nzBd(sumProductCogs(supplierId, productId, geo, previousStart, previousEnd));

        ProductAnalysisSummaryMetricsV2DTO dto = buildSummaryMetricsDto(
                activeCustomersCurrent, activeCustomersPrevious,
                ordersCurrent, ordersPrevious,
                tonnesCurrent, tonnesPrevious,
                cogsCurrent, cogsPrevious);
        dto.setProductImageUrl(resolveProductImageUrl(productId));
        dto.setRanking(salesOrdersRepository.findSupplierPortalProductBestSellingRankByCogs(
                supplierId, productId,
                geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                currentStart, currentEnd));
        return dto;
    }

    @Override
    public ProductAnalysisSalesTrendV2DTO getSalesTrend(UUID productId,
                                                       List<UUID> regionIds,
                                                       List<UUID> locationIds,
                                                       Integer month,
                                                       Integer year,
                                                       Principal principal) {
        SupplierPortalTrendPeriod period = SupplierPortalProductAnalysisValidation.resolveSalesTrendPeriod(
                month, year, LocalDate.now(EAST_AFRICA));
        Suppliers supplier = resolveSupplier(principal);
        assertProductOwnedBySupplier(productId, supplier);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);
        return buildProductSalesTrend(supplier.getPublicId(), productId, geo, period);
    }

    @Override
    public ProductAnalysisDistrictRankingV2DTO getDistrictRanking(UUID productId,
                                                                  List<UUID> regionIds,
                                                                  List<UUID> locationIds,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate,
                                                                  Integer limit,
                                                                  Principal principal) {
        SupplierPortalProductAnalysisValidation.validateDateRange(startDate, endDate);
        int effectiveLimit = limit != null ? limit : DEFAULT_DISTRICT_RANKING_LIMIT;
        SupplierPortalProductAnalysisValidation.validateDistrictRankingLimit(effectiveLimit);
        Suppliers supplier = resolveSupplier(principal);
        assertProductOwnedBySupplier(productId, supplier);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        LocalDateTime from = dateStartInclusive(startDate);
        LocalDateTime to = dateEndInclusive(endDate);
        UUID supplierId = supplier.getPublicId();
        List<Object[]> raw = geo.usesWardRankingBucket()
                ? salesOrdersRepository.listSupplierPortalProductWardRankingBySettledTonnes(
                        supplierId, productId,
                        geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(), from, to)
                : salesOrdersRepository.listSupplierPortalProductRegionRankingBySettledTonnes(
                        supplierId, productId, from, to);

        List<ProductAnalysisDistrictRankingRowDTO> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] row : raw) {
            if (rank > effectiveLimit) {
                break;
            }
            ProductAnalysisDistrictRankingRowDTO dto = new ProductAnalysisDistrictRankingRowDTO();
            dto.setRank(rank++);
            dto.setOrganizationLocationPublicId(coerceUuid(row[0]));
            dto.setOrganizationLocationName(row[1] != null ? row[1].toString() : "UNKNOWN");
            dto.setSettledTonnes(nzBd(coerceToBigDecimal(row[2])));
            rows.add(dto);
        }

        ProductAnalysisDistrictRankingV2DTO response = new ProductAnalysisDistrictRankingV2DTO();
        response.setRows(rows);
        return response;
    }

    @Override
    public ProductAnalysisDistrictCogsV2DTO getDistrictCogs(UUID productId,
                                                            List<UUID> regionIds,
                                                            List<UUID> locationIds,
                                                            LocalDate startDate,
                                                            LocalDate endDate,
                                                            Integer limit,
                                                            Principal principal) {
        SupplierPortalProductAnalysisValidation.validateDateRange(startDate, endDate);
        int effectiveLimit = limit != null ? limit : DEFAULT_DISTRICT_RANKING_LIMIT;
        SupplierPortalProductAnalysisValidation.validateDistrictRankingLimit(effectiveLimit);
        Suppliers supplier = resolveSupplier(principal);
        assertProductOwnedBySupplier(productId, supplier);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        LocalDateTime from = dateStartInclusive(startDate);
        LocalDateTime to = dateEndInclusive(endDate);
        UUID supplierId = supplier.getPublicId();

        boolean wardBucket = geo.usesWardRankingBucket();
        List<Object[]> raw = wardBucket
                ? salesOrdersRepository.listSupplierPortalProductWardCogsByTotalCogs(
                        supplierId, productId,
                        geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(), from, to)
                : salesOrdersRepository.listSupplierPortalProductRegionCogsByTotalCogs(
                        supplierId, productId, from, to);

        BigDecimal totalCogsTzs = nzBd(sumProductCogs(supplierId, productId, geo, from, to));

        List<ProductAnalysisDistrictCogsRowDTO> rows = new ArrayList<>();
        int rowCount = 0;
        for (Object[] row : raw) {
            if (++rowCount > effectiveLimit) {
                break;
            }
            ProductAnalysisDistrictCogsRowDTO dto = new ProductAnalysisDistrictCogsRowDTO();
            dto.setOrganizationLocationPublicId(coerceUuid(row[0]));
            dto.setOrganizationLocationName(row[1] != null ? row[1].toString() : "UNKNOWN");
            dto.setTotalCogsTzs(nzBd(coerceToBigDecimal(row[2])));
            rows.add(dto);
        }

        ProductAnalysisDistrictCogsV2DTO response = new ProductAnalysisDistrictCogsV2DTO();
        response.setRows(rows);
        response.setTotalCogsTzs(totalCogsTzs);
        return response;
    }

    @Override
    public Page<ProductAnalysisWardPerformanceRowDTO> getWardPerformance(UUID productId,
                                                                         List<UUID> regionIds,
                                                                         List<UUID> locationIds,
                                                                         LocalDate startDate,
                                                                         LocalDate endDate,
                                                                         Principal principal,
                                                                         Pageable pageable) {
        SupplierPortalProductAnalysisValidation.validateDateRange(startDate, endDate);
        SupplierPortalProductAnalysisValidation.validateWardPerformancePageable(pageable);
        Suppliers supplier = resolveSupplier(principal);
        assertProductOwnedBySupplier(productId, supplier);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        LocalDateTime currentFrom = dateStartInclusive(startDate);
        LocalDateTime currentTo = dateEndInclusive(endDate);
        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousFrom = dateStartInclusive(prev[0]);
        LocalDateTime previousTo = dateEndInclusive(prev[1]);

        UUID supplierId = supplier.getPublicId();
        Map<String, WardRankingSnapshot> previousByKey = mapWardPerformanceByKey(
                salesOrdersRepository.listSupplierPortalProductWardPerformanceV2(
                        supplierId, productId,
                        geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                        previousFrom, previousTo));

        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Object[]> raw = salesOrdersRepository.pageSupplierPortalProductWardPerformanceV2(
                supplierId, productId,
                geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                currentFrom, currentTo, sanitized);

        return buildWardPerformancePage(raw, previousByKey);
    }

    @Override
    public ProductAnalysisSalesCalendarV2DTO getSalesCalendar(UUID productId,
                                                              List<UUID> regionIds,
                                                              List<UUID> locationIds,
                                                              Integer month,
                                                              Integer year,
                                                              Principal principal) {
        SupplierPortalProductAnalysisValidation.validateSalesCalendarMonthYear(month, year);
        Suppliers supplier = resolveSupplier(principal);
        assertProductOwnedBySupplier(productId, supplier);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        YearMonth calendarMonth = YearMonth.of(year, month);
        LocalDate startDate = calendarMonth.atDay(1);
        LocalDate endDate = calendarMonth.atEndOfMonth();
        LocalDateTime from = dateStartInclusive(startDate);
        LocalDateTime to = dateEndInclusive(endDate);
        UUID supplierId = supplier.getPublicId();

        List<Object[]> raw = salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
                supplierId, productId,
                geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                from, to);

        long totalOrders = nz(countProductDeliveredOrders(supplierId, productId, geo, from, to));
        BigDecimal totalQuantitySoldTonnes = nzBd(sumProductSettledTonnes(supplierId, productId, geo, from, to));
        BigDecimal totalCogsTzs = nzBd(sumProductCogs(supplierId, productId, geo, from, to));

        YearMonth previousMonth = calendarMonth.minusMonths(1);
        LocalDateTime previousFrom = dateStartInclusive(previousMonth.atDay(1));
        LocalDateTime previousTo = dateEndInclusive(previousMonth.atEndOfMonth());
        List<Object[]> previousRaw = salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
                supplierId, productId,
                geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                previousFrom, previousTo);
        long previousOrders = nz(countProductDeliveredOrders(supplierId, productId, geo, previousFrom, previousTo));
        BigDecimal previousQuantitySoldTonnes = nzBd(sumProductSettledTonnes(supplierId, productId, geo, previousFrom, previousTo));
        BigDecimal previousPeakDayCogs = resolvePeakDayCogs(previousRaw, previousMonth.atDay(1), previousMonth.atEndOfMonth());

        return buildSalesCalendarDto(month, year, raw, startDate, endDate,
                totalOrders, totalQuantitySoldTonnes, totalCogsTzs,
                previousOrders, previousQuantitySoldTonnes, previousPeakDayCogs);
    }

    @Override
    public ProductAnalysisSummaryMetricsV2DTO getAllProductsSummaryMetrics(List<UUID> regionIds,
                                                                           List<UUID> locationIds,
                                                                           LocalDate startDate,
                                                                           LocalDate endDate,
                                                                           Principal principal) {
        SupplierPortalProductAnalysisValidation.validateDateRange(startDate, endDate);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        LocalDateTime currentStart = dateStartInclusive(startDate);
        LocalDateTime currentEnd = dateEndInclusive(endDate);
        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousStart = dateStartInclusive(prev[0]);
        LocalDateTime previousEnd = dateEndInclusive(prev[1]);

        UUID supplierId = supplier.getPublicId();
        long activeCustomersCurrent = nz(countDistinctDeliveredCustomers(supplierId, geo, currentStart, currentEnd));
        long activeCustomersPrevious = nz(countDistinctDeliveredCustomers(supplierId, geo, previousStart, previousEnd));

        long ordersCurrent = nz(countAllProductsDeliveredOrders(supplierId, geo, currentStart, currentEnd));
        long ordersPrevious = nz(countAllProductsDeliveredOrders(supplierId, geo, previousStart, previousEnd));

        BigDecimal tonnesCurrent = nzBd(sumAllProductsSettledTonnes(supplierId, geo, currentStart, currentEnd));
        BigDecimal tonnesPrevious = nzBd(sumAllProductsSettledTonnes(supplierId, geo, previousStart, previousEnd));

        BigDecimal cogsCurrent = nzBd(sumSupplierCogs(supplierId, geo, currentStart, currentEnd));
        BigDecimal cogsPrevious = nzBd(sumSupplierCogs(supplierId, geo, previousStart, previousEnd));

        return buildSummaryMetricsDto(
                activeCustomersCurrent, activeCustomersPrevious,
                ordersCurrent, ordersPrevious,
                tonnesCurrent, tonnesPrevious,
                cogsCurrent, cogsPrevious);
    }

    @Override
    public ProductAnalysisSalesTrendV2DTO getAllProductsSalesTrend(List<UUID> regionIds,
                                                                 List<UUID> locationIds,
                                                                 Integer month,
                                                                 Integer year,
                                                                 Principal principal) {
        SupplierPortalTrendPeriod period = SupplierPortalProductAnalysisValidation.resolveSalesTrendPeriod(
                month, year, LocalDate.now(EAST_AFRICA));
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);
        return buildAllProductsSalesTrend(supplier.getPublicId(), geo, period);
    }

    @Override
    public ProductAnalysisDistrictRankingV2DTO getAllProductsDistrictRanking(List<UUID> regionIds,
                                                                             List<UUID> locationIds,
                                                                             LocalDate startDate,
                                                                             LocalDate endDate,
                                                                             Integer limit,
                                                                             Principal principal) {
        SupplierPortalProductAnalysisValidation.validateDateRange(startDate, endDate);
        int effectiveLimit = limit != null ? limit : DEFAULT_DISTRICT_RANKING_LIMIT;
        SupplierPortalProductAnalysisValidation.validateDistrictRankingLimit(effectiveLimit);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        UUID supplierId = supplier.getPublicId();
        LocalDateTime from = dateStartInclusive(startDate);
        LocalDateTime to = dateEndInclusive(endDate);
        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);

        boolean wardBucket = geo.usesWardRankingBucket();
        List<Object[]> raw = wardBucket
                ? salesOrdersRepository.listSupplierPortalAllProductsWardRankingBySettledTonnes(
                        supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(), from, to)
                : salesOrdersRepository.listSupplierPortalAllProductsRegionRankingBySettledTonnes(
                        supplierId, from, to);
        LocalDateTime previousFrom = dateStartInclusive(prev[0]);
        LocalDateTime previousTo = dateEndInclusive(prev[1]);
        List<Object[]> previousRaw = wardBucket
                ? salesOrdersRepository.listSupplierPortalAllProductsWardRankingBySettledTonnes(
                        supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(), previousFrom, previousTo)
                : salesOrdersRepository.listSupplierPortalAllProductsRegionRankingBySettledTonnes(
                        supplierId, previousFrom, previousTo);
        BigDecimal totalSettledTonnes = nzBd(sumAllProductsSettledTonnes(supplierId, geo, from, to));

        Map<String, WardRankingSnapshot> previousByKey = wardBucket
                ? mapWardRankingByKey(previousRaw)
                : mapRegionRankingByKey(previousRaw);
        return buildDistrictRankingDto(raw, previousByKey, effectiveLimit, totalSettledTonnes, wardBucket);
    }

    @Override
    public ProductAnalysisDistrictCogsV2DTO getAllProductsDistrictCogs(List<UUID> regionIds,
                                                                       List<UUID> locationIds,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate,
                                                                       Integer limit,
                                                                       Principal principal) {
        SupplierPortalProductAnalysisValidation.validateDateRange(startDate, endDate);
        int effectiveLimit = limit != null ? limit : DEFAULT_DISTRICT_RANKING_LIMIT;
        SupplierPortalProductAnalysisValidation.validateDistrictRankingLimit(effectiveLimit);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        LocalDateTime from = dateStartInclusive(startDate);
        LocalDateTime to = dateEndInclusive(endDate);
        UUID supplierId = supplier.getPublicId();

        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);

        boolean wardBucket = geo.usesWardRankingBucket();
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        List<Object[]> raw = wardBucket
                ? salesOrdersRepository.listSupplierPortalAllProductsWardCogsByTotalCogs(
                        supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                        products.productIdsParam(), products.productFilterDisabledParam(), from, to)
                : salesOrdersRepository.listSupplierPortalAllProductsRegionCogsByTotalCogs(
                        supplierId, from, to);
        LocalDateTime previousFrom = dateStartInclusive(prev[0]);
        LocalDateTime previousTo = dateEndInclusive(prev[1]);
        List<Object[]> previousRaw = wardBucket
                ? salesOrdersRepository.listSupplierPortalAllProductsWardCogsByTotalCogs(
                        supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                        products.productIdsParam(), products.productFilterDisabledParam(), previousFrom, previousTo)
                : salesOrdersRepository.listSupplierPortalAllProductsRegionCogsByTotalCogs(
                        supplierId, previousFrom, previousTo);

        BigDecimal totalCogsTzs = nzBd(sumSupplierCogs(supplierId, geo, from, to));

        Map<String, BigDecimal> previousCogsByKey = wardBucket
                ? mapWardCogsByKey(previousRaw)
                : mapRegionCogsByKey(previousRaw);
        return buildDistrictCogsDto(raw, previousCogsByKey, effectiveLimit, totalCogsTzs, wardBucket);
    }

    @Override
    public Page<ProductAnalysisWardPerformanceRowDTO> getWardPerformanceForExport(UUID productId,
                                                                                  List<UUID> regionIds,
                                                                                  List<UUID> locationIds,
                                                                                  LocalDate startDate,
                                                                                  LocalDate endDate,
                                                                                  Principal principal,
                                                                                  Pageable pageable) {
        SupplierPortalProductAnalysisValidation.validateDateRange(startDate, endDate);
        SupplierPortalProductAnalysisValidation.validateExportPageable(pageable);
        Suppliers supplier = resolveSupplier(principal);
        assertProductOwnedBySupplier(productId, supplier);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        LocalDateTime currentFrom = dateStartInclusive(startDate);
        LocalDateTime currentTo = dateEndInclusive(endDate);
        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousFrom = dateStartInclusive(prev[0]);
        LocalDateTime previousTo = dateEndInclusive(prev[1]);

        UUID supplierId = supplier.getPublicId();
        Map<String, WardRankingSnapshot> previousByKey = mapWardPerformanceByKey(
                salesOrdersRepository.listSupplierPortalProductWardPerformanceV2(
                        supplierId, productId,
                        geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                        previousFrom, previousTo));

        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Object[]> raw = salesOrdersRepository.pageSupplierPortalProductWardPerformanceV2(
                supplierId, productId,
                geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                currentFrom, currentTo, sanitized);

        return buildWardPerformancePage(raw, previousByKey);
    }

    @Override
    public Page<ProductAnalysisWardPerformanceRowDTO> getAllProductsWardPerformance(List<UUID> regionIds,
                                                                                  List<UUID> locationIds,
                                                                                  LocalDate startDate,
                                                                                  LocalDate endDate,
                                                                                  Principal principal,
                                                                                  Pageable pageable) {
        SupplierPortalProductAnalysisValidation.validateDateRange(startDate, endDate);
        SupplierPortalProductAnalysisValidation.validateWardPerformancePageable(pageable);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        LocalDateTime currentFrom = dateStartInclusive(startDate);
        LocalDateTime currentTo = dateEndInclusive(endDate);
        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousFrom = dateStartInclusive(prev[0]);
        LocalDateTime previousTo = dateEndInclusive(prev[1]);

        UUID supplierId = supplier.getPublicId();
        Map<String, WardRankingSnapshot> previousByKey = mapWardPerformanceByKey(
                salesOrdersRepository.listSupplierPortalAllProductsWardPerformanceV2(
                        supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(), previousFrom, previousTo));

        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Object[]> raw = salesOrdersRepository.pageSupplierPortalAllProductsWardPerformanceV2(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), currentFrom, currentTo, sanitized);

        return buildWardPerformancePage(raw, previousByKey);
    }

    @Override
    public Page<ProductAnalysisProductRankingCogsRowV2DTO> getAllProductsProductRankingCogs(List<UUID> regionIds,
                                                                                            List<UUID> locationIds,
                                                                                            LocalDate startDate,
                                                                                            LocalDate endDate,
                                                                                            Principal principal,
                                                                                            Pageable pageable) {
        SupplierPortalProductAnalysisValidation.validateDateRange(startDate, endDate);
        SupplierPortalProductAnalysisValidation.validateProductRankingCogsUiPageable(pageable);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        LocalDateTime currentFrom = dateStartInclusive(startDate);
        LocalDateTime currentTo = dateEndInclusive(endDate);
        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousFrom = dateStartInclusive(prev[0]);
        LocalDateTime previousTo = dateEndInclusive(prev[1]);

        UUID supplierId = supplier.getPublicId();
        Pageable uiPageable = PageRequest.of(
                0, SupplierPortalProductAnalysisValidation.PRODUCT_RANKING_COGS_UI_TOP_N);
        Page<Object[]> raw = salesOrdersRepository.pageSupplierPortalAllProductsProductRankingByCogs(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), currentFrom, currentTo, uiPageable);

        Map<String, BigDecimal> previousCogsByProductId = mapProductCogsByKey(
                salesOrdersRepository.listSupplierPortalAllProductsProductRankingByCogs(
                        supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(), previousFrom, previousTo));

        List<ProductAnalysisProductRankingCogsRowV2DTO> mapped = new ArrayList<>(raw.getContent().size());
        for (int i = 0; i < raw.getContent().size(); i++) {
            ProductAnalysisProductRankingCogsRowV2DTO dto = mapProductRankingCogsRow(raw.getContent().get(i));
            dto.setRank(i + 1);
            BigDecimal previousCogs = dto.getProductPublicId() != null
                    ? previousCogsByProductId.get(dto.getProductPublicId().toString())
                    : null;
            dto.setPeriodTrends(buildProductCogsPeriodTrends(dto.getTotalCogsTzs(), previousCogs));
            mapped.add(dto);
        }
        enrichProductRankingImagesBestEffort(mapped);
        long uiTotalElements = Math.min(
                raw.getTotalElements(),
                SupplierPortalProductAnalysisValidation.PRODUCT_RANKING_COGS_UI_TOP_N);
        return new PageImpl<>(mapped, uiPageable, uiTotalElements);
    }

    @Override
    public Page<ProductAnalysisWardPerformanceRowDTO> getAllProductsWardPerformanceForExport(List<UUID> regionIds,
                                                                                             List<UUID> locationIds,
                                                                                             LocalDate startDate,
                                                                                             LocalDate endDate,
                                                                                             Principal principal,
                                                                                             Pageable pageable) {
        SupplierPortalProductAnalysisValidation.validateDateRange(startDate, endDate);
        SupplierPortalProductAnalysisValidation.validateExportPageable(pageable);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        LocalDateTime currentFrom = dateStartInclusive(startDate);
        LocalDateTime currentTo = dateEndInclusive(endDate);
        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousFrom = dateStartInclusive(prev[0]);
        LocalDateTime previousTo = dateEndInclusive(prev[1]);

        UUID supplierId = supplier.getPublicId();
        Map<String, WardRankingSnapshot> previousByKey = mapWardPerformanceByKey(
                salesOrdersRepository.listSupplierPortalAllProductsWardPerformanceV2(
                        supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(), previousFrom, previousTo));

        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Object[]> raw = salesOrdersRepository.pageSupplierPortalAllProductsWardPerformanceV2(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), currentFrom, currentTo, sanitized);

        return buildWardPerformancePage(raw, previousByKey);
    }

    @Override
    public Page<ProductAnalysisProductRankingCogsRowV2DTO> getAllProductsProductRankingCogsForExport(List<UUID> regionIds,
                                                                                                     List<UUID> locationIds,
                                                                                                     LocalDate startDate,
                                                                                                     LocalDate endDate,
                                                                                                     Principal principal,
                                                                                                     Pageable pageable) {
        SupplierPortalProductAnalysisValidation.validateDateRange(startDate, endDate);
        SupplierPortalProductAnalysisValidation.validateExportPageable(pageable);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        LocalDateTime currentFrom = dateStartInclusive(startDate);
        LocalDateTime currentTo = dateEndInclusive(endDate);
        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousFrom = dateStartInclusive(prev[0]);
        LocalDateTime previousTo = dateEndInclusive(prev[1]);

        UUID supplierId = supplier.getPublicId();
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Object[]> raw = salesOrdersRepository.pageSupplierPortalAllProductsProductRankingByCogs(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), currentFrom, currentTo, sanitized);

        Map<String, BigDecimal> previousCogsByProductId = mapProductCogsByKey(
                salesOrdersRepository.listSupplierPortalAllProductsProductRankingByCogs(
                        supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(), previousFrom, previousTo));

        int rankBase = (int) raw.getPageable().getOffset();
        List<ProductAnalysisProductRankingCogsRowV2DTO> mapped = new ArrayList<>(raw.getContent().size());
        for (int i = 0; i < raw.getContent().size(); i++) {
            ProductAnalysisProductRankingCogsRowV2DTO dto = mapProductRankingCogsRow(raw.getContent().get(i));
            dto.setRank(rankBase + i + 1);
            BigDecimal previousCogs = dto.getProductPublicId() != null
                    ? previousCogsByProductId.get(dto.getProductPublicId().toString())
                    : null;
            dto.setPeriodTrends(buildProductCogsPeriodTrends(dto.getTotalCogsTzs(), previousCogs));
            mapped.add(dto);
        }
        enrichProductRankingImagesBestEffort(mapped);
        return new PageImpl<>(mapped, sanitized, raw.getTotalElements());
    }

    @Override
    public ProductAnalysisSalesCalendarV2DTO getAllProductsSalesCalendar(List<UUID> regionIds,
                                                                         List<UUID> locationIds,
                                                                         Integer month,
                                                                         Integer year,
                                                                         Principal principal) {
        SupplierPortalProductAnalysisValidation.validateSalesCalendarMonthYear(month, year);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        YearMonth calendarMonth = YearMonth.of(year, month);
        LocalDate startDate = calendarMonth.atDay(1);
        LocalDate endDate = calendarMonth.atEndOfMonth();
        LocalDateTime from = dateStartInclusive(startDate);
        LocalDateTime to = dateEndInclusive(endDate);

        List<Object[]> raw = salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                supplier.getPublicId(), geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), from, to);

        UUID supplierId = supplier.getPublicId();
        long totalOrders = nz(countAllProductsDeliveredOrders(supplierId, geo, from, to));
        BigDecimal totalQuantitySoldTonnes = nzBd(sumAllProductsSettledTonnes(supplierId, geo, from, to));
        BigDecimal totalCogsTzs = nzBd(sumSupplierCogs(supplierId, geo, from, to));

        YearMonth previousMonth = calendarMonth.minusMonths(1);
        LocalDateTime previousFrom = dateStartInclusive(previousMonth.atDay(1));
        LocalDateTime previousTo = dateEndInclusive(previousMonth.atEndOfMonth());
        List<Object[]> previousRaw = salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), previousFrom, previousTo);
        long previousOrders = nz(countAllProductsDeliveredOrders(supplierId, geo, previousFrom, previousTo));
        BigDecimal previousQuantitySoldTonnes = nzBd(sumAllProductsSettledTonnes(supplierId, geo, previousFrom, previousTo));
        BigDecimal previousPeakDayCogs = resolvePeakDayCogs(previousRaw, previousMonth.atDay(1), previousMonth.atEndOfMonth());

        return buildSalesCalendarDto(month, year, raw, startDate, endDate,
                totalOrders, totalQuantitySoldTonnes, totalCogsTzs,
                previousOrders, previousQuantitySoldTonnes, previousPeakDayCogs);
    }

    private ProductAnalysisSummaryMetricsV2DTO buildSummaryMetricsDto(long activeCustomersCurrent,
                                                                      long activeCustomersPrevious,
                                                                      long ordersCurrent,
                                                                      long ordersPrevious,
                                                                      BigDecimal tonnesCurrent,
                                                                      BigDecimal tonnesPrevious,
                                                                      BigDecimal cogsCurrent,
                                                                      BigDecimal cogsPrevious) {
        ProductAnalysisSummaryMetricsV2DTO dto = new ProductAnalysisSummaryMetricsV2DTO();
        dto.setActiveCustomers(new TrendMetricDTO<>(
                activeCustomersCurrent,
                SupplierPortalDashboardMath.percentChangeLong(activeCustomersCurrent, activeCustomersPrevious),
                LABEL_PREVIOUS_PERIOD));
        dto.setTotalOrders(new TrendMetricDTO<>(
                ordersCurrent,
                SupplierPortalDashboardMath.percentChangeLong(ordersCurrent, ordersPrevious),
                LABEL_PREVIOUS_PERIOD));
        dto.setTotalQuantitySoldTonnes(new TrendMetricDTO<>(
                tonnesCurrent,
                SupplierPortalDashboardMath.percentChange(tonnesCurrent, tonnesPrevious),
                LABEL_PREVIOUS_PERIOD));
        dto.setTotalCogsTzs(new TrendMetricDTO<>(
                cogsCurrent,
                SupplierPortalDashboardMath.percentChange(cogsCurrent, cogsPrevious),
                LABEL_PREVIOUS_PERIOD));
        BigDecimal aovCurrent = computeAverageOrderValueTzs(cogsCurrent, ordersCurrent);
        BigDecimal aovPrevious = computeAverageOrderValueTzs(cogsPrevious, ordersPrevious);
        dto.setAverageOrderValueTzs(new TrendMetricDTO<>(
                aovCurrent,
                SupplierPortalDashboardMath.percentChange(aovCurrent, aovPrevious),
                LABEL_PREVIOUS_PERIOD));
        return dto;
    }

    private String resolveProductImageUrl(UUID productPublicId) {
        return productImageResolver.resolve(productPublicId);
    }

    /**
     * Portal V2 AOV: total line COGS ÷ distinct delivered order count (same scope as summary cards).
     */
    private BigDecimal computeAverageOrderValueTzs(BigDecimal cogs, long orderCount) {
        if (orderCount == 0L) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return nzBd(cogs).divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
    }

    private ProductAnalysisSalesTrendV2DTO buildProductSalesTrend(UUID supplierId,
                                                                 UUID productId,
                                                                 SupplierPortalGeographyFilter geo,
                                                                 SupplierPortalTrendPeriod period) {
        if (period.granularity() == SupplierPortalTrendGranularity.DAILY_IN_CALENDAR_MONTH) {
            LocalDateTime from = dateStartInclusive(period.startInclusive());
            LocalDateTime to = dateEndInclusive(period.endInclusive());
            List<Object[]> raw = salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
                    supplierId, productId,
                    geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                    geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                    from, to);
            BigDecimal previousTotal = sumProductCogsForPreviousCalendarMonth(
                    supplierId, productId, geo, period);
            return buildSalesTrendDto(raw, period.startInclusive(), period.endInclusive(),
                    null, previousTotal, LABEL_PREVIOUS_PERIOD);
        }

        List<Object[]> raw = salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationMonthForProduct(
                supplierId, productId,
                geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                dateStartInclusive(period.startInclusive()),
                dateEndInclusive(period.endInclusive()));
        List<ProductAnalysisDailyCogsPointDTO> series = buildMonthlySalesTrendSeries(period.year(), raw);
        BigDecimal currentTotal = series.stream()
                .map(ProductAnalysisDailyCogsPointDTO::getTotalCogsTzs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate[] prevYear = SupplierPortalTrendPeriodValidation.previousCalendarYearWindow(period.year());
        BigDecimal previousTotal = nzBd(sumProductCogs(supplierId, productId, geo,
                dateStartInclusive(prevYear[0]), dateEndInclusive(prevYear[1])));
        return buildSalesTrendDtoFromSeries(series, currentTotal, previousTotal, LABEL_VS_LAST_YEAR);
    }

    private ProductAnalysisSalesTrendV2DTO buildAllProductsSalesTrend(UUID supplierId,
                                                                      SupplierPortalGeographyFilter geo,
                                                                      SupplierPortalTrendPeriod period) {
        if (period.granularity() == SupplierPortalTrendGranularity.DAILY_IN_CALENDAR_MONTH) {
            LocalDateTime from = dateStartInclusive(period.startInclusive());
            LocalDateTime to = dateEndInclusive(period.endInclusive());
            List<Object[]> raw = salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                    supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                    geo.locationIdsParam(), geo.locationFilterDisabledParam(), from, to);
            BigDecimal previousTotal = sumAllProductsCogsForPreviousCalendarMonth(
                    supplierId, geo, period);
            return buildSalesTrendDto(raw, period.startInclusive(), period.endInclusive(),
                    null, previousTotal, LABEL_PREVIOUS_PERIOD);
        }

        List<Object[]> raw = salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                dateStartInclusive(period.startInclusive()),
                dateEndInclusive(period.endInclusive()));
        List<ProductAnalysisDailyCogsPointDTO> series = buildMonthlySalesTrendSeries(period.year(), raw);
        BigDecimal currentTotal = series.stream()
                .map(ProductAnalysisDailyCogsPointDTO::getTotalCogsTzs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate[] prevYear = SupplierPortalTrendPeriodValidation.previousCalendarYearWindow(period.year());
        BigDecimal previousTotal = nzBd(sumSupplierCogs(supplierId, geo,
                dateStartInclusive(prevYear[0]), dateEndInclusive(prevYear[1])));
        return buildSalesTrendDtoFromSeries(series, currentTotal, previousTotal, LABEL_VS_LAST_YEAR);
    }

    private BigDecimal sumProductCogsForPreviousCalendarMonth(UUID supplierId,
                                                              UUID productId,
                                                              SupplierPortalGeographyFilter geo,
                                                              SupplierPortalTrendPeriod period) {
        YearMonth previous = SupplierPortalTrendPeriodValidation.previousCalendarMonth(
                YearMonth.of(period.year(), period.month()));
        return nzBd(sumProductCogs(supplierId, productId, geo,
                dateStartInclusive(previous.atDay(1)),
                dateEndInclusive(previous.atEndOfMonth())));
    }

    private BigDecimal sumAllProductsCogsForPreviousCalendarMonth(UUID supplierId,
                                                                  SupplierPortalGeographyFilter geo,
                                                                  SupplierPortalTrendPeriod period) {
        YearMonth previous = SupplierPortalTrendPeriodValidation.previousCalendarMonth(
                YearMonth.of(period.year(), period.month()));
        return nzBd(sumSupplierCogs(supplierId, geo,
                dateStartInclusive(previous.atDay(1)),
                dateEndInclusive(previous.atEndOfMonth())));
    }

    private List<ProductAnalysisDailyCogsPointDTO> buildMonthlySalesTrendSeries(int year, List<Object[]> raw) {
        Map<Integer, DailySalesTrendAggregate> byMonth = new HashMap<>();
        for (Object[] row : raw) {
            int month = intFromJdbcNumber(row[2]);
            byMonth.put(month, new DailySalesTrendAggregate(
                    nzBd(coerceToBigDecimal(row[0])),
                    row.length > 3 ? coerceLong(row[3]) : 0L,
                    row.length > 4 ? coerceLong(row[4]) : 0L));
        }

        List<ProductAnalysisDailyCogsPointDTO> series = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            DailySalesTrendAggregate agg = byMonth.getOrDefault(month, DailySalesTrendAggregate.ZERO);
            series.add(new ProductAnalysisDailyCogsPointDTO(
                    LocalDate.of(year, month, 1),
                    agg.cogsTzs(),
                    agg.orderCount(),
                    agg.customerCount()));
        }
        return series;
    }

    private static int intFromJdbcNumber(Object raw) {
        if (raw == null) {
            return 0;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Expected numeric JDBC type, got " + raw.getClass());
    }

    private ProductAnalysisSalesTrendV2DTO buildSalesTrendDtoFromSeries(List<ProductAnalysisDailyCogsPointDTO> series,
                                                                        BigDecimal currentTotal,
                                                                        BigDecimal previousTotal,
                                                                        String trendLabel) {
        ProductAnalysisSalesTrendV2DTO dto = new ProductAnalysisSalesTrendV2DTO();
        dto.setSeries(series);
        dto.setHeadline(new TrendMetricDTO<>(
                currentTotal,
                SupplierPortalDashboardMath.percentChange(currentTotal, previousTotal),
                trendLabel));
        return dto;
    }

    private ProductAnalysisSalesTrendV2DTO buildSalesTrendDto(List<Object[]> raw,
                                                              LocalDate startDate,
                                                              LocalDate endDate,
                                                              BigDecimal currentTotal,
                                                              BigDecimal previousTotal,
                                                              String trendLabel) {
        Map<LocalDate, DailySalesTrendAggregate> byDay = new HashMap<>();
        for (Object[] row : raw) {
            LocalDate day = coerceSqlDate(row[1]);
            byDay.put(day, new DailySalesTrendAggregate(
                    nzBd(coerceToBigDecimal(row[0])),
                    row.length > 2 ? coerceLong(row[2]) : 0L,
                    row.length > 3 ? coerceLong(row[3]) : 0L));
        }

        List<ProductAnalysisDailyCogsPointDTO> series = new ArrayList<>();
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            DailySalesTrendAggregate agg = byDay.getOrDefault(day, DailySalesTrendAggregate.ZERO);
            series.add(new ProductAnalysisDailyCogsPointDTO(
                    day, agg.cogsTzs(), agg.orderCount(), agg.customerCount()));
        }

        BigDecimal windowTotal = series.stream()
                .map(ProductAnalysisDailyCogsPointDTO::getTotalCogsTzs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return buildSalesTrendDtoFromSeries(series, windowTotal, previousTotal, trendLabel);
    }

    private record DailySalesTrendAggregate(BigDecimal cogsTzs, long orderCount, long customerCount) {
        private static final DailySalesTrendAggregate ZERO =
                new DailySalesTrendAggregate(BigDecimal.ZERO, 0L, 0L);
    }

    private ProductAnalysisDistrictRankingV2DTO buildDistrictRankingDto(List<Object[]> raw,
                                                                        Map<String, WardRankingSnapshot> previousByKey,
                                                                        int effectiveLimit,
                                                                        BigDecimal totalSettledTonnes,
                                                                        boolean wardBucket) {
        List<ProductAnalysisDistrictRankingRowDTO> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] row : raw) {
            if (rank > effectiveLimit) {
                break;
            }
            WardRankingSnapshot current = wardRankingFromRow(row);
            ProductAnalysisDistrictRankingRowDTO dto = new ProductAnalysisDistrictRankingRowDTO();
            dto.setRank(rank++);
            dto.setOrganizationLocationPublicId(current.locationPublicId());
            dto.setOrganizationLocationName(current.locationName());
            dto.setSettledTonnes(current.settledTonnes());
            dto.setTotalCogsTzs(current.totalCogsTzs());
            dto.setDeliveredOrderCount(current.deliveredOrderCount());
            dto.setAverageOrderValueTzs(current.averageOrderValueTzs());
            String mergeKey = wardBucket ? wardMergeKey(current) : regionMergeKey(current);
            dto.setPeriodTrends(buildWardSalesDistributionPeriodTrends(
                    current, previousByKey.get(mergeKey)));
            rows.add(dto);
        }

        ProductAnalysisDistrictRankingV2DTO response = new ProductAnalysisDistrictRankingV2DTO();
        response.setRows(rows);
        response.setTotalSettledTonnes(totalSettledTonnes);
        return response;
    }

    private ProductAnalysisDistrictCogsV2DTO buildDistrictCogsDto(List<Object[]> raw,
                                                                   Map<String, BigDecimal> previousCogsByKey,
                                                                   int effectiveLimit,
                                                                   BigDecimal totalCogsTzs,
                                                                   boolean wardBucket) {
        List<ProductAnalysisDistrictCogsRowDTO> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] row : raw) {
            if (rank > effectiveLimit) {
                break;
            }
            UUID bucketId = coerceUuid(row[0]);
            String bucketName = row[1] != null ? row[1].toString() : "UNKNOWN";
            BigDecimal cogs = nzBd(coerceToBigDecimal(row[2]));
            ProductAnalysisDistrictCogsRowDTO dto = new ProductAnalysisDistrictCogsRowDTO();
            dto.setRank(rank++);
            dto.setOrganizationLocationPublicId(bucketId);
            dto.setOrganizationLocationName(bucketName);
            dto.setTotalCogsTzs(cogs);
            String mergeKey = wardBucket
                    ? wardMergeKey(bucketId, bucketName)
                    : regionMergeKey(bucketId, bucketName);
            dto.setPeriodTrends(buildWardCogsPeriodTrends(cogs, previousCogsByKey.get(mergeKey)));
            rows.add(dto);
        }

        ProductAnalysisDistrictCogsV2DTO response = new ProductAnalysisDistrictCogsV2DTO();
        response.setRows(rows);
        response.setTotalCogsTzs(totalCogsTzs);
        return response;
    }

    private record WardRankingSnapshot(UUID locationPublicId,
                                       String locationName,
                                       BigDecimal settledTonnes,
                                       BigDecimal totalCogsTzs,
                                       long deliveredOrderCount,
                                       BigDecimal averageOrderValueTzs) {
    }

    private WardRankingSnapshot wardRankingFromRow(Object[] row) {
        UUID locationPublicId = coerceUuid(row[0]);
        String locationName = row[1] != null ? row[1].toString() : "UNKNOWN";
        BigDecimal settledTonnes = nzBd(coerceToBigDecimal(row[2]));
        BigDecimal totalCogsTzs = row.length > 3 ? nzBd(coerceToBigDecimal(row[3])) : BigDecimal.ZERO;
        long deliveredOrderCount = row.length > 4 ? coerceLong(row[4]) : 0L;
        BigDecimal averageOrderValueTzs = computeAverageOrderValueTzs(totalCogsTzs, deliveredOrderCount);
        return new WardRankingSnapshot(
                locationPublicId, locationName, settledTonnes, totalCogsTzs, deliveredOrderCount, averageOrderValueTzs);
    }

    private Map<String, WardRankingSnapshot> mapWardRankingByKey(List<Object[]> raw) {
        Map<String, WardRankingSnapshot> byKey = new HashMap<>();
        for (Object[] row : raw) {
            WardRankingSnapshot snapshot = wardRankingFromRow(row);
            byKey.put(wardMergeKey(snapshot), snapshot);
        }
        return byKey;
    }

    private Map<String, WardRankingSnapshot> mapRegionRankingByKey(List<Object[]> raw) {
        Map<String, WardRankingSnapshot> byKey = new HashMap<>();
        for (Object[] row : raw) {
            WardRankingSnapshot snapshot = wardRankingFromRow(row);
            byKey.put(regionMergeKey(snapshot), snapshot);
        }
        return byKey;
    }


    private Long countProductActiveCustomers(UUID supplierId, UUID productId, SupplierPortalGeographyFilter geo,
                                             LocalDateTime from, LocalDateTime to) {
        return salesOrdersRepository.countActiveCustomersBySupplierProductAndGeography(
                supplierId, productId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), from, to);
    }

    private Long countProductDeliveredOrders(UUID supplierId, UUID productId, SupplierPortalGeographyFilter geo,
                                             LocalDateTime from, LocalDateTime to) {
        return salesOrdersRepository.countDeliveredOrdersBySupplierProductAndGeography(
                supplierId, productId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), from, to);
    }

    private BigDecimal sumProductSettledTonnes(UUID supplierId, UUID productId, SupplierPortalGeographyFilter geo,
                                               LocalDateTime from, LocalDateTime to) {
        return salesOrdersRepository.sumSettledTonnesBySupplierProductAndGeography(
                supplierId, productId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), from, to);
    }

    private BigDecimal sumProductCogs(UUID supplierId, UUID productId, SupplierPortalGeographyFilter geo,
                                      LocalDateTime from, LocalDateTime to) {
        return salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                supplierId, productId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), from, to);
    }

    private Long countDistinctDeliveredCustomers(UUID supplierId, SupplierPortalGeographyFilter geo,
                                                 LocalDateTime from, LocalDateTime to) {
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        return salesOrdersRepository.countDistinctDeliveredSupplierPortalCustomers(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to);
    }

    private Long countAllProductsDeliveredOrders(UUID supplierId, SupplierPortalGeographyFilter geo,
                                                 LocalDateTime from, LocalDateTime to) {
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        return salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to);
    }

    private BigDecimal sumAllProductsSettledTonnes(UUID supplierId, SupplierPortalGeographyFilter geo,
                                                   LocalDateTime from, LocalDateTime to) {
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        return salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to);
    }

    private BigDecimal sumSupplierCogs(UUID supplierId, SupplierPortalGeographyFilter geo,
                                       LocalDateTime from, LocalDateTime to) {
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        return salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to);
    }

    private Map<String, BigDecimal> mapWardCogsByKey(List<Object[]> raw) {
        Map<String, BigDecimal> byKey = new HashMap<>();
        for (Object[] row : raw) {
            UUID locationPublicId = coerceUuid(row[0]);
            String locationName = row[1] != null ? row[1].toString() : "UNKNOWN";
            byKey.put(wardMergeKey(locationPublicId, locationName), nzBd(coerceToBigDecimal(row[2])));
        }
        return byKey;
    }

    private Map<String, BigDecimal> mapRegionCogsByKey(List<Object[]> raw) {
        Map<String, BigDecimal> byKey = new HashMap<>();
        for (Object[] row : raw) {
            UUID regionPublicId = coerceUuid(row[0]);
            String regionName = row[1] != null ? row[1].toString() : "UNKNOWN";
            byKey.put(regionMergeKey(regionPublicId, regionName), nzBd(coerceToBigDecimal(row[2])));
        }
        return byKey;
    }

    private static String wardMergeKey(WardRankingSnapshot snapshot) {
        return wardMergeKey(snapshot.locationPublicId(), snapshot.locationName());
    }

    private static String wardMergeKey(UUID locationPublicId, String locationName) {
        if (locationPublicId != null) {
            return locationPublicId.toString();
        }
        return "UNKNOWN:" + (locationName != null ? locationName : "");
    }

    private static String regionMergeKey(WardRankingSnapshot snapshot) {
        return regionMergeKey(snapshot.locationPublicId(), snapshot.locationName());
    }

    private static String regionMergeKey(UUID regionPublicId, String regionName) {
        if (regionPublicId != null) {
            return regionPublicId.toString();
        }
        return "UNKNOWN:" + (regionName != null ? regionName : "");
    }

    private ProductAnalysisWardPeriodTrendsDTO buildWardCogsPeriodTrends(BigDecimal currentCogs, BigDecimal previousCogs) {
        return buildProductCogsPeriodTrends(currentCogs, previousCogs);
    }

    private static ProductAnalysisWardPeriodTrendsDTO buildProductCogsPeriodTrends(
            BigDecimal currentCogs,
            BigDecimal previousCogs) {
        ProductAnalysisWardPeriodTrendsDTO trends = new ProductAnalysisWardPeriodTrendsDTO();
        trends.setTrendLabel(LABEL_PREVIOUS_PERIOD);
        trends.setTotalCogsTzsTrendPercentage(
                SupplierPortalDashboardMath.percentChange(
                        currentCogs, previousCogs != null ? previousCogs : BigDecimal.ZERO));
        return trends;
    }

    private Map<String, BigDecimal> mapProductCogsByKey(List<Object[]> rows) {
        Map<String, BigDecimal> byKey = new HashMap<>();
        for (Object[] row : rows) {
            UUID productId = coerceUuid(row[0]);
            if (productId != null) {
                byKey.put(productId.toString(), nzBd(coerceToBigDecimal(row[5])));
            }
        }
        return byKey;
    }

    private ProductAnalysisWardPeriodTrendsDTO buildWardSalesDistributionPeriodTrends(
            WardRankingSnapshot current,
            WardRankingSnapshot previous) {
        return buildWardPerformancePeriodTrends(current, previous);
    }

    /**
     * Ward performance / sales-distribution trends vs the immediately preceding equal-length window.
     * COGS trend uses the same {@link SupplierPortalDashboardMath#percentChange} formula as product-ranking-cogs.
     */
    private ProductAnalysisWardPeriodTrendsDTO buildWardPerformancePeriodTrends(
            WardRankingSnapshot current,
            WardRankingSnapshot previous) {
        ProductAnalysisWardPeriodTrendsDTO trends = new ProductAnalysisWardPeriodTrendsDTO();
        trends.setTrendLabel(LABEL_PREVIOUS_PERIOD);
        BigDecimal previousCogs = previous != null ? previous.totalCogsTzs() : BigDecimal.ZERO;
        BigDecimal previousTonnes = previous != null ? previous.settledTonnes() : BigDecimal.ZERO;
        BigDecimal previousAov = previous != null ? previous.averageOrderValueTzs() : BigDecimal.ZERO;
        trends.setTotalCogsTzsTrendPercentage(
                SupplierPortalDashboardMath.percentChange(current.totalCogsTzs(), previousCogs));
        trends.setSettledTonnesTrendPercentage(
                SupplierPortalDashboardMath.percentChange(current.settledTonnes(), previousTonnes));
        trends.setAverageOrderValueTzsTrendPercentage(
                SupplierPortalDashboardMath.percentChange(current.averageOrderValueTzs(), previousAov));
        return trends;
    }

    private Page<ProductAnalysisWardPerformanceRowDTO> buildWardPerformancePage(
            Page<Object[]> raw,
            Map<String, WardRankingSnapshot> previousByKey) {
        Pageable pageable = raw.getPageable();
        int rankBase = pageable.isPaged() ? (int) pageable.getOffset() : 0;
        List<ProductAnalysisWardPerformanceRowDTO> mapped = new ArrayList<>(raw.getContent().size());
        for (int i = 0; i < raw.getContent().size(); i++) {
            WardRankingSnapshot current = wardPerformanceSnapshotFromRow(raw.getContent().get(i));
            ProductAnalysisWardPerformanceRowDTO dto = mapWardPerformanceRow(raw.getContent().get(i), current);
            dto.setRank(rankBase + i + 1);
            dto.setAverageOrderValueTzs(current.averageOrderValueTzs());
            dto.setPeriodTrends(buildWardPerformancePeriodTrends(
                    current, previousByKey.get(wardMergeKey(current))));
            mapped.add(dto);
        }
        return new PageImpl<>(mapped, pageable, raw.getTotalElements());
    }

    private WardRankingSnapshot wardPerformanceSnapshotFromRow(Object[] row) {
        UUID locationPublicId = coerceUuid(row[0]);
        String locationName = row[1] != null ? row[1].toString() : "UNKNOWN";
        BigDecimal totalCogsTzs = nzBd(coerceToBigDecimal(row[2]));
        BigDecimal settledTonnes = nzBd(coerceToBigDecimal(row[3]));
        long deliveredOrderCount = row.length > 5 ? coerceLong(row[5]) : 0L;
        BigDecimal averageOrderValueTzs = computeAverageOrderValueTzs(totalCogsTzs, deliveredOrderCount);
        return new WardRankingSnapshot(
                locationPublicId, locationName, settledTonnes, totalCogsTzs, deliveredOrderCount, averageOrderValueTzs);
    }

    private Map<String, WardRankingSnapshot> mapWardPerformanceByKey(List<Object[]> raw) {
        Map<String, WardRankingSnapshot> byKey = new HashMap<>();
        for (Object[] row : raw) {
            WardRankingSnapshot snapshot = wardPerformanceSnapshotFromRow(row);
            byKey.put(wardMergeKey(snapshot), snapshot);
        }
        return byKey;
    }

    private ProductAnalysisSalesCalendarV2DTO buildSalesCalendarDto(Integer month,
                                                                    Integer year,
                                                                    List<Object[]> raw,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate,
                                                                    long totalOrders,
                                                                    BigDecimal totalQuantitySoldTonnes,
                                                                    BigDecimal totalCogsTzs,
                                                                    long previousOrders,
                                                                    BigDecimal previousQuantitySoldTonnes,
                                                                    BigDecimal previousPeakDayCogs) {
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        for (Object[] row : raw) {
            LocalDate day = coerceSqlDate(row[1]);
            byDay.put(day, nzBd(coerceToBigDecimal(row[0])));
        }

        List<ProductAnalysisDailyCogsPointDTO> days = new ArrayList<>();
        LocalDate peakDate = null;
        BigDecimal peakDayCogs = BigDecimal.ZERO;
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            BigDecimal dayCogs = byDay.getOrDefault(day, BigDecimal.ZERO);
            days.add(new ProductAnalysisDailyCogsPointDTO(day, dayCogs));
            if (dayCogs.compareTo(peakDayCogs) > 0) {
                peakDayCogs = dayCogs;
                peakDate = day;
            }
        }

        ProductAnalysisSalesCalendarV2DTO dto = new ProductAnalysisSalesCalendarV2DTO();
        dto.setMonth(month);
        dto.setYear(year);
        dto.setDays(days);
        dto.setTotalOrders(totalOrders);
        dto.setTotalQuantitySoldTonnes(totalQuantitySoldTonnes);
        dto.setTotalCogsTzs(totalCogsTzs);
        dto.setMonthlyOrdersVersusPreviousMonth(new TrendMetricDTO<>(
                totalOrders,
                SupplierPortalDashboardMath.percentChangeLongOrZero(totalOrders, previousOrders),
                LABEL_VS_PREVIOUS_MONTH));
        dto.setMonthlyUnitsSoldVersusPreviousMonth(new TrendMetricDTO<>(
                totalQuantitySoldTonnes,
                SupplierPortalDashboardMath.percentChangeOrZero(totalQuantitySoldTonnes, previousQuantitySoldTonnes),
                LABEL_VS_PREVIOUS_MONTH));
        if (peakDate != null) {
            ProductAnalysisSalesCalendarPeakDayV2DTO peakDay = new ProductAnalysisSalesCalendarPeakDayV2DTO();
            peakDay.setDate(peakDate);
            peakDay.setTotalCogsTzs(peakDayCogs);
            peakDay.setTotalCogsTzsVersusPreviousMonth(new TrendMetricDTO<>(
                    peakDayCogs,
                    SupplierPortalDashboardMath.percentChangeOrZero(peakDayCogs, previousPeakDayCogs),
                    LABEL_VS_PREVIOUS_MONTH));
            dto.setPeakDay(peakDay);
        }
        return dto;
    }

    private BigDecimal resolvePeakDayCogs(List<Object[]> raw, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        for (Object[] row : raw) {
            LocalDate day = coerceSqlDate(row[1]);
            byDay.put(day, nzBd(coerceToBigDecimal(row[0])));
        }

        BigDecimal peakDayCogs = BigDecimal.ZERO;
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            BigDecimal dayCogs = byDay.getOrDefault(day, BigDecimal.ZERO);
            if (dayCogs.compareTo(peakDayCogs) > 0) {
                peakDayCogs = dayCogs;
            }
        }
        return peakDayCogs;
    }

    private void enrichProductRankingImagesBestEffort(List<ProductAnalysisProductRankingCogsRowV2DTO> rows) {
        for (ProductAnalysisProductRankingCogsRowV2DTO dto : rows) {
            dto.setProductImageUrl(resolveProductImageUrl(dto.getProductPublicId()));
        }
    }

    private ProductAnalysisProductRankingCogsRowV2DTO mapProductRankingCogsRow(Object[] row) {
        ProductAnalysisProductRankingCogsRowV2DTO dto = new ProductAnalysisProductRankingCogsRowV2DTO();
        dto.setProductPublicId(coerceUuid(row[0]));
        dto.setProductName(row[1] != null ? row[1].toString() : null);
        dto.setUnitOfMeasure(row[3] != null ? row[3].toString() : null);
        BigDecimal totalCogsTzs = nzBd(coerceToBigDecimal(row[5]));
        dto.setTotalCogsTzs(totalCogsTzs);
        dto.setTotalCogsTzsLabel(SupplierPortalDashboardMath.formatTzsLabel(totalCogsTzs));
        dto.setTotalQuantitySoldUnits(BigDecimal.valueOf(coerceLong(row[4])));
        return dto;
    }

    private ProductAnalysisWardPerformanceRowDTO mapWardPerformanceRow(
            Object[] row,
            WardRankingSnapshot snapshot) {
        ProductAnalysisWardPerformanceRowDTO dto = new ProductAnalysisWardPerformanceRowDTO();
        dto.setOrganizationLocationPublicId(snapshot.locationPublicId());
        dto.setOrganizationLocationName(snapshot.locationName());
        dto.setTotalCogsTzs(snapshot.totalCogsTzs());
        dto.setSettledTonnes(snapshot.settledTonnes());
        dto.setDistinctCustomerCount(coerceLong(row[4]));
        dto.setDeliveredOrderCount(snapshot.deliveredOrderCount());
        return dto;
    }

    private void assertProductOwnedBySupplier(UUID productId, Suppliers supplier) {
        if (productId == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("productId required for Product Analysis V2")
                    .userMessage("Product is required");
        }
        if (!productRepository.existsByPublicIdAndSupplierPublicId(productId, supplier.getPublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("product not linked to supplier: " + productId)
                    .userMessage("Product not found");
        }
    }

    private Suppliers resolveSupplier(Principal principal) {
        Person person = personRepository.findByEmailAndDeletedIsFalse(principal.getName())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! user not found")
                        .userMessage("Sorry! user not found"));

        String supplierPublicId = Optional.ofNullable(person.getSupplierPublicId())
                .filter(id -> !id.isEmpty())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! no associated supplier")
                        .userMessage("Sorry! no associated supplier"));

        return supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(supplierPublicId))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sorry! Supplier not found")
                        .userMessage("Sorry! Supplier not found"));
    }

    private LocalDateTime dateStartInclusive(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime dateEndInclusive(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    private long nz(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal nzBd(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static UUID coerceUuid(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof UUID uuid) {
            return uuid;
        }
        throw new IllegalArgumentException("Expected UUID from JDBC bridge, got " + raw.getClass());
    }

    private static BigDecimal coerceToBigDecimal(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof BigDecimal bd) {
            return bd;
        }
        if (raw instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        throw new IllegalArgumentException("Expected numeric JDBC type, got " + raw.getClass());
    }

    private static long coerceLong(Object raw) {
        if (raw == null) {
            return 0L;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Expected numeric JDBC type, got " + raw.getClass());
    }

    private static LocalDate coerceSqlDate(Object raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Expected date bucket from JDBC");
        }
        if (raw instanceof LocalDate localDate) {
            return localDate;
        }
        if (raw instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (raw instanceof java.util.Date utilDate) {
            return new Date(utilDate.getTime()).toLocalDate();
        }
        throw new IllegalArgumentException("Expected date JDBC type, got " + raw.getClass());
    }
}
