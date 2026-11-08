package com.app.itibari.services;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.report.DistrictPerformanceV2PageDTO;
import com.app.itibari.models.report.DistrictPerformanceV2RowDTO;
import com.app.itibari.models.report.DistrictPerformanceV2TrendsDTO;
import com.app.itibari.models.report.ProductDistributionV2RowDTO;
import com.app.itibari.models.report.ProductDistributionV2TrendsDTO;
import com.app.itibari.models.report.SalesCalendarDayV2DTO;
import com.app.itibari.models.report.SalesCalendarSummaryV2DTO;
import com.app.itibari.models.report.SalesCalendarV2DTO;
import com.app.itibari.models.report.SalesRevenueDTO;
import com.app.itibari.models.report.SalesTrendV2DTO;
import com.app.itibari.models.report.SummaryCardsV2DTO;
import com.app.itibari.models.report.SupplierPortalDashboardSummaryMetricsRow;
import com.app.itibari.models.report.TrendMetricDTO;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.services.helper.ProductImageResolver;
import com.app.itibari.services.supplierportal.SupplierPortalDashboardMath;
import com.app.itibari.services.supplierportal.SupplierPortalDashboardValidation;
import com.app.itibari.services.supplierportal.SupplierPortalGeographyFilter;
import com.app.itibari.services.supplierportal.SupplierPortalProductFilter;
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
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Supplier Portal V2 dashboards — isolated analytics that intentionally bypass {@link ReportService}
 * while mirroring operational semantics (supplier scoping, DELIVERED filter, Nairobi calendar anchor).
 * <p>Supplier COGS/revenue is the sum of {@code pricePerUnit * quantityDelivered} for that supplier's delivered lines only
 * (mixed carts stay correct because each line is attributed by product supplier). Discounts and order-level taxes/fees are
 * out of scope for portal reporting unless product asks for them explicitly. Quantity-sold tonnes use separate repository logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierPortalDashboardServiceImpl implements SupplierPortalDashboardService {

    private static final ZoneId EAST_AFRICA = ZoneId.of("Africa/Dar_es_Salaam");
    private static final String LABEL_PREVIOUS_PERIOD = "vs Previous Period";
    private static final String LABEL_VS_LAST_YEAR = "vs Last Year";

    private static final String LABEL_VS_PREVIOUS_MONTH = "vs Previous Month";

    private final SalesOrdersRepository salesOrdersRepository;
    private final PersonRepository personRepository;
    private final SupplierRepository supplierRepository;
    private final ProductImageResolver productImageResolver;

    /**
     * Summary cards use one consolidated aggregate per window (current + previous).
     * Customer/order counts preserve today's summary-metrics output for demo safety; DISTINCT V2 alignment is a follow-up.
     */
    @Override
    public SummaryCardsV2DTO getDashboardSummaryMetrics(List<UUID> regionIds,
                                                        List<UUID> locationIds,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        Principal principal) {
        validateDateRangeForV2Portal(startDate, endDate);

        Optional<Suppliers> supplierOptional = findAssociatedSupplier(principal);
        if (supplierOptional.isEmpty()) {
            return emptySummaryCards();
        }
        Suppliers supplier = supplierOptional.get();
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);
        LocalDateTime currentStartTime = dateStartInclusive(startDate);
        LocalDateTime currentEndTime = dateEndInclusive(endDate);

        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousStartTime = dateStartInclusive(prev[0]);
        LocalDateTime previousEndTime = dateEndInclusive(prev[1]);

        SupplierPortalDashboardSummaryMetricsRow currentMetrics = loadDashboardSummaryMetrics(
                supplier.getPublicId(), geo, currentStartTime, currentEndTime);
        SupplierPortalDashboardSummaryMetricsRow previousMetrics = loadDashboardSummaryMetrics(
                supplier.getPublicId(), geo, previousStartTime, previousEndTime);

        SummaryCardsV2DTO dto = new SummaryCardsV2DTO();
        dto.setActiveCustomers(new TrendMetricDTO<>(
                currentMetrics.activeCustomers(),
                SupplierPortalDashboardMath.percentChangeLongOrZero(
                        currentMetrics.activeCustomers(), previousMetrics.activeCustomers()),
                LABEL_PREVIOUS_PERIOD));
        dto.setTotalOrders(new TrendMetricDTO<>(
                currentMetrics.totalOrders(),
                SupplierPortalDashboardMath.percentChangeLongOrZero(
                        currentMetrics.totalOrders(), previousMetrics.totalOrders()),
                LABEL_PREVIOUS_PERIOD));
        dto.setTotalQuantitySoldTonnes(new TrendMetricDTO<>(
                currentMetrics.totalQuantitySoldTonnes(),
                SupplierPortalDashboardMath.percentChangeOrZero(
                        currentMetrics.totalQuantitySoldTonnes(), previousMetrics.totalQuantitySoldTonnes()),
                LABEL_PREVIOUS_PERIOD));
        dto.setTotalCogsTzs(new TrendMetricDTO<>(
                currentMetrics.totalCogsTzs(),
                SupplierPortalDashboardMath.percentChangeOrZero(
                        currentMetrics.totalCogsTzs(), previousMetrics.totalCogsTzs()),
                LABEL_PREVIOUS_PERIOD));
        return dto;
    }

    public SummaryCardsV2DTO getDashboardSummaryMetrics(UUID regionId,
                                                        UUID locationId,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        Principal principal) {
        return getDashboardSummaryMetrics(optionalUuidList(regionId), optionalUuidList(locationId),
                startDate, endDate, principal);
    }

    private SupplierPortalDashboardSummaryMetricsRow loadDashboardSummaryMetrics(UUID supplierId,
                                                                                 SupplierPortalGeographyFilter geo,
                                                                                 LocalDateTime fromTs,
                                                                                 LocalDateTime toTs) {
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        return new SupplierPortalDashboardSummaryMetricsRow(
                nz(salesOrdersRepository.countDistinctDeliveredSupplierPortalCustomers(
                        supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                        products.productIdsParam(), products.productFilterDisabledParam(), fromTs, toTs)),
                nz(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                        supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                        products.productIdsParam(), products.productFilterDisabledParam(), fromTs, toTs)),
                nzBd(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                        supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                        products.productIdsParam(), products.productFilterDisabledParam(), fromTs, toTs)),
                nzBd(sumSupplierCogs(supplierId, geo, fromTs, toTs)));
    }

    @Override
    public SalesTrendV2DTO getSalesTrend(List<UUID> regionIds,
                                         List<UUID> locationIds,
                                         Integer month,
                                         Integer year,
                                         Boolean monthly,
                                         Boolean quarterly,
                                         Boolean yearly,
                                         Principal principal,
                                         Pageable pageable) {
        SupplierPortalTrendPeriodValidation.rejectLegacyGranularityFlags(monthly, quarterly, yearly);
        LocalDate today = LocalDate.now(EAST_AFRICA);
        SupplierPortalTrendPeriod period = SupplierPortalTrendPeriodValidation.resolve(
                month, year, today, "revenue-trend");
        Optional<Suppliers> supplierOptional = findAssociatedSupplier(principal);
        if (supplierOptional.isEmpty()) {
            return emptySalesTrend(period);
        }
        UUID supplierId = supplierOptional.get().getPublicId();
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);

        List<SalesRevenueDTO> series;
        TrendMetricDTO<BigDecimal> headline;
        if (period.granularity() == SupplierPortalTrendGranularity.DAILY_IN_CALENDAR_MONTH) {
            series = buildDailyRevenueTrendSeries(supplierId, geo, period);
            headline = buildRevenueTrendHeadlineForCalendarMonth(supplierId, geo, period, series);
        } else {
            List<Object[]> monthBuckets = loadMonthBuckets(
                    supplierId, geo, period.startInclusive(), period.endInclusive());
            series = zeroFillMonthlyRevenueSeries(period.year(), monthBuckets);
            headline = buildRevenueTrendHeadlineForCalendarYear(supplierId, geo, period, series);
        }

        SalesTrendV2DTO dto = new SalesTrendV2DTO();
        dto.setSeries(series);
        dto.setMonthToDateCogsVersusLastYear(headline);
        return dto;
    }

    @Override
    public Page<ProductDistributionV2RowDTO> getProductDistributionV2(List<UUID> regionIds,
                                                                      List<UUID> locationIds,
                                                                      LocalDate startDate,
                                                                      LocalDate endDate,
                                                                      Principal principal,
                                                                      Pageable pageable) {
        validateDateRangeForV2Portal(startDate, endDate);
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Optional<Suppliers> supplierOptional = findAssociatedSupplier(principal);
        if (supplierOptional.isEmpty()) {
            return new PageImpl<>(List.of(), sanitized, 0);
        }
        Suppliers supplier = supplierOptional.get();
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);
        LocalDateTime currentFrom = dateStartInclusive(startDate);
        LocalDateTime currentTo = dateEndInclusive(endDate);

        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousFrom = dateStartInclusive(prev[0]);
        LocalDateTime previousTo = dateEndInclusive(prev[1]);

        UUID supplierId = supplier.getPublicId();
        Page<Object[]> raw = salesOrdersRepository.pageSupplierPortalProductDistributionV2(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), currentFrom, currentTo, sanitized);

        List<Object[]> previousRows = salesOrdersRepository.listSupplierPortalProductDistributionV2(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), previousFrom, previousTo);

        Map<String, ProductDistributionV2RowDTO> previousByProductId = new HashMap<>();
        for (Object[] row : previousRows) {
            ProductDistributionV2RowDTO mapped = mapProductDistributionRow(row);
            if (mapped.getProductPublicId() != null) {
                previousByProductId.put(mapped.getProductPublicId().toString(), mapped);
            }
        }

        int rankBase = (int) raw.getPageable().getOffset();
        List<ProductDistributionV2RowDTO> mapped = new ArrayList<>(raw.getContent().size());
        for (int i = 0; i < raw.getContent().size(); i++) {
            ProductDistributionV2RowDTO dto = mapProductDistributionRow(raw.getContent().get(i));
            dto.setRank(rankBase + i + 1);
            String mergeKey = dto.getProductPublicId() != null ? dto.getProductPublicId().toString() : null;
            dto.setPeriodTrends(buildProductPeriodTrends(dto, mergeKey != null ? previousByProductId.get(mergeKey) : null));
            mapped.add(dto);
        }

        enrichProductImagesBestEffort(mapped);

        return new PageImpl<>(mapped, sanitized, raw.getTotalElements());
    }

    public Page<ProductDistributionV2RowDTO> getProductDistributionV2(UUID regionId,
                                                                      UUID locationId,
                                                                      LocalDate startDate,
                                                                      LocalDate endDate,
                                                                      Principal principal,
                                                                      Pageable pageable) {
        return getProductDistributionV2(optionalUuidList(regionId), optionalUuidList(locationId),
                startDate, endDate, principal, pageable);
    }

    /**
     * District-performance table: region (district) rows only when both filters are absent; ward rows when
     * {@code regionId} and/or {@code locationId} is set (same rule as Customer Analysis district ranking).
     * {@link DistrictPerformanceV2RowDTO#getOrganizationLocationPublicId()} / {@link DistrictPerformanceV2RowDTO#getOrganizationLocationName()}
     * carry region identity in district mode and ward identity in ward mode.
     */
    @Override
    public DistrictPerformanceV2PageDTO getDistrictPerformanceV2(List<UUID> regionIds,
                                                                      List<UUID> locationIds,
                                                                      LocalDate startDate,
                                                                      LocalDate endDate,
                                                                      Principal principal,
                                                                      Pageable pageable) {
        validateDateRangeForV2Portal(startDate, endDate);
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Optional<Suppliers> supplierOptional = findAssociatedSupplier(principal);
        if (supplierOptional.isEmpty()) {
            return DistrictPerformanceV2PageDTO.from(new PageImpl<>(List.of(), sanitized, 0), BigDecimal.ZERO);
        }
        Suppliers supplier = supplierOptional.get();
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);
        LocalDateTime currentFrom = dateStartInclusive(startDate);
        LocalDateTime currentTo = dateEndInclusive(endDate);

        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousFrom = dateStartInclusive(prev[0]);
        LocalDateTime previousTo = dateEndInclusive(prev[1]);

        UUID supplierId = supplier.getPublicId();
        boolean wardMode = geo.usesWardRankingBucket();
        Page<Object[]> currentPage;
        List<Object[]> previousRows;
        if (wardMode) {
            currentPage = salesOrdersRepository.pageSupplierPortalAllProductsWardPerformanceV2(
                    supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                    geo.locationIdsParam(), geo.locationFilterDisabledParam(), currentFrom, currentTo, sanitized);
            previousRows = salesOrdersRepository.listSupplierPortalAllProductsWardPerformanceV2(
                    supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                    geo.locationIdsParam(), geo.locationFilterDisabledParam(), previousFrom, previousTo);
        } else {
            currentPage = salesOrdersRepository.pageSupplierPortalDistrictPerformanceByRegionV2(
                    supplierId, null, currentFrom, currentTo, sanitized);
            previousRows = salesOrdersRepository.listSupplierPortalDistrictPerformanceByRegionV2(
                    supplierId, null, previousFrom, previousTo);
        }

        Map<String, DistrictPerformanceV2RowDTO> previousByKey = new HashMap<>();
        for (Object[] row : previousRows) {
            DistrictPerformanceV2RowDTO mapped = mapDistrictRow(row);
            previousByKey.put(districtMergeKey(mapped), mapped);
        }

        List<DistrictPerformanceV2RowDTO> mapped = new ArrayList<>(currentPage.getContent().size());
        for (Object[] row : currentPage.getContent()) {
            DistrictPerformanceV2RowDTO dto = mapDistrictRow(row);
            dto.setPeriodTrends(buildDistrictPeriodTrends(dto, previousByKey.get(districtMergeKey(dto))));
            mapped.add(dto);
        }
        Page<DistrictPerformanceV2RowDTO> page = new PageImpl<>(mapped, sanitized, currentPage.getTotalElements());
        BigDecimal totalSupplierLineRevenueTzs = nzBd(sumSupplierCogs(supplierId, geo, currentFrom, currentTo));
        return DistrictPerformanceV2PageDTO.from(page, totalSupplierLineRevenueTzs);
    }

    public DistrictPerformanceV2PageDTO getDistrictPerformanceV2(UUID regionId,
                                                                      UUID locationId,
                                                                      LocalDate startDate,
                                                                      LocalDate endDate,
                                                                      Principal principal,
                                                                      Pageable pageable) {
        return getDistrictPerformanceV2(optionalUuidList(regionId), optionalUuidList(locationId),
                startDate, endDate, principal, pageable);
    }

    @Override
    public SalesCalendarV2DTO getSalesCalendar(List<UUID> regionIds,
                                               List<UUID> locationIds,
                                               int month,
                                               int year,
                                               Principal principal) {
        SupplierPortalDashboardValidation.validateCalendarMonthYear(month, year);

        YearMonth selectedMonth = YearMonth.of(year, month);
        Optional<Suppliers> supplierOptional = findAssociatedSupplier(principal);
        if (supplierOptional.isEmpty()) {
            return emptySalesCalendar(selectedMonth);
        }
        Suppliers supplier = supplierOptional.get();
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);
        LocalDate monthStart = selectedMonth.atDay(1);
        LocalDate monthEnd = selectedMonth.atEndOfMonth();
        LocalDateTime fromTs = dateStartInclusive(monthStart);
        LocalDateTime toTs = dateEndInclusive(monthEnd);

        Map<LocalDate, DayAggregate> aggregatesByDate = mapSalesCalendarRows(
                salesOrdersRepository.aggregateSupplierPortalSalesCalendarByDay(
                        supplier.getPublicId(), geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(), fromTs, toTs));

        BigDecimal monthPeakRevenue = aggregatesByDate.values().stream()
                .map(DayAggregate::revenueTzs)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        List<SalesCalendarDayV2DTO> days = new ArrayList<>(selectedMonth.lengthOfMonth());
        BigDecimal totalTonnes = BigDecimal.ZERO;
        long totalOrders = 0L;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        LocalDate peakDay = null;
        BigDecimal peakDayRevenue = BigDecimal.ZERO;

        for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
            DayAggregate aggregate = aggregatesByDate.getOrDefault(date, DayAggregate.zero());
            days.add(toSalesCalendarDay(date, aggregate, monthPeakRevenue));

            totalTonnes = totalTonnes.add(aggregate.tonnes());
            totalOrders += aggregate.orderCount();
            totalRevenue = totalRevenue.add(aggregate.revenueTzs());

            if (aggregate.revenueTzs().compareTo(peakDayRevenue) > 0) {
                peakDayRevenue = aggregate.revenueTzs();
                peakDay = date;
            }
        }

        YearMonth previousMonth = selectedMonth.minusMonths(1);
        LocalDateTime previousFromTs = dateStartInclusive(previousMonth.atDay(1));
        LocalDateTime previousToTs = dateEndInclusive(previousMonth.atEndOfMonth());

        BigDecimal previousRevenue = nzBd(sumSupplierCogs(
                supplier.getPublicId(), geo,
                previousFromTs, previousToTs));
        long previousOrders = nz(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                supplier.getPublicId(), geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                SupplierPortalProductFilter.none().productIdsParam(),
                SupplierPortalProductFilter.none().productFilterDisabledParam(),
                previousFromTs, previousToTs));
        BigDecimal previousTonnes = nzBd(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                supplier.getPublicId(), geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                SupplierPortalProductFilter.none().productIdsParam(),
                SupplierPortalProductFilter.none().productFilterDisabledParam(),
                previousFromTs, previousToTs));

        SalesCalendarSummaryV2DTO summary = new SalesCalendarSummaryV2DTO();
        summary.setPeakDay(peakDayRevenue.compareTo(BigDecimal.ZERO) > 0 ? peakDay : null);
        summary.setPeakDayRevenueTzs(peakDayRevenue);
        summary.setPeakDayRevenueLabel(SupplierPortalDashboardMath.formatTzsLabel(peakDayRevenue));
        summary.setUnitsSoldTonnes(totalTonnes);
        summary.setUnitsSoldLabel(SupplierPortalDashboardMath.formatTonnesLabel(totalTonnes));
        summary.setTotalOrders(totalOrders);
        summary.setMonthlyRevenueVersusPreviousMonth(new TrendMetricDTO<>(
                totalRevenue,
                SupplierPortalDashboardMath.percentChangeOrZero(totalRevenue, previousRevenue),
                LABEL_VS_PREVIOUS_MONTH));
        summary.setMonthlyOrdersVersusPreviousMonth(new TrendMetricDTO<>(
                totalOrders,
                SupplierPortalDashboardMath.percentChangeLongOrZero(totalOrders, previousOrders),
                LABEL_VS_PREVIOUS_MONTH));
        summary.setMonthlyUnitsSoldVersusPreviousMonth(new TrendMetricDTO<>(
                totalTonnes,
                SupplierPortalDashboardMath.percentChangeOrZero(totalTonnes, previousTonnes),
                LABEL_VS_PREVIOUS_MONTH));

        SalesCalendarV2DTO dto = new SalesCalendarV2DTO();
        dto.setMonth(month);
        dto.setYear(year);
        dto.setMonthLabel(selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        dto.setSummary(summary);
        dto.setDays(days);
        return dto;
    }

    public SalesCalendarV2DTO getSalesCalendar(UUID regionId,
                                               UUID locationId,
                                               int month,
                                               int year,
                                               Principal principal) {
        return getSalesCalendar(optionalUuidList(regionId), optionalUuidList(locationId), month, year, principal);
    }

    private void validateDateRangeForV2Portal(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("startDate/endDate required for Supplier Portal V2 analytics")
                    .userMessage("Date range required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestRestApiException()
                    .developerMessage("endDate before startDate")
                    .userMessage("Invalid date range");
        }
    }

    /** Resolves {@code productImageUrl} / {@code imageUrl} per row (FileService, with local demo placeholders). */
    private void enrichProductImagesBestEffort(List<ProductDistributionV2RowDTO> rows) {
        productImageResolver.enrichRows(
                rows,
                ProductDistributionV2RowDTO::getProductPublicId,
                ProductDistributionV2RowDTO::getSku,
                ProductDistributionV2RowDTO::setProductImageUrl);
    }

    private static ProductDistributionV2RowDTO mapProductDistributionRow(Object[] r) {
        ProductDistributionV2RowDTO dto = new ProductDistributionV2RowDTO();
        dto.setProductPublicId(coerceUuid(r[0]));
        dto.setProductName(r[1] != null ? r[1].toString() : null);
        dto.setSku(r[2] != null ? r[2].toString() : null);
        dto.setUom(r[3] != null ? r[3].toString() : null);
        dto.setUnitsSold(coerceLong(r[4]));
        dto.setSupplierLineRevenueTzs(nzBdStatic(coerceToBigDecimal(r[5])));
        dto.setSettledTonnes(nzBdStatic(coerceToBigDecimal(r[6])));
        return dto;
    }

    private static Map<LocalDate, DayAggregate> mapSalesCalendarRows(List<Object[]> rows) {
        Map<LocalDate, DayAggregate> mapped = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate date = coerceLocalDate(row[0]);
            mapped.put(date, new DayAggregate(
                    nzBdStatic(coerceToBigDecimal(row[1])),
                    nzBdStatic(coerceToBigDecimal(row[2])),
                    coerceLong(row[3])));
        }
        return mapped;
    }

    private static SalesCalendarDayV2DTO toSalesCalendarDay(LocalDate date,
                                                            DayAggregate aggregate,
                                                            BigDecimal monthPeakRevenue) {
        double intensity = SupplierPortalDashboardMath.intensityRatio(aggregate.revenueTzs(), monthPeakRevenue);

        SalesCalendarDayV2DTO dto = new SalesCalendarDayV2DTO();
        dto.setDate(date);
        dto.setRevenueTzs(aggregate.revenueTzs());
        dto.setRevenueLabel(SupplierPortalDashboardMath.formatTzsLabel(aggregate.revenueTzs()));
        dto.setQuantitySoldTonnes(aggregate.tonnes());
        dto.setQuantitySoldLabel(SupplierPortalDashboardMath.formatTonnesLabel(aggregate.tonnes()));
        dto.setCogsTzs(aggregate.revenueTzs());
        dto.setCogsLabel(SupplierPortalDashboardMath.formatTzsLabel(aggregate.revenueTzs()));
        dto.setOrderCount(aggregate.orderCount());
        dto.setIntensity(intensity);
        dto.setIntensityLevel(SupplierPortalDashboardMath.intensityLevel(intensity));
        return dto;
    }

    private static LocalDate coerceLocalDate(Object raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Expected calendar date from JDBC bridge");
        }
        if (raw instanceof LocalDate localDate) {
            return localDate;
        }
        if (raw instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (raw instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (raw instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (raw instanceof java.time.Instant instant) {
            return instant.atZone(EAST_AFRICA).toLocalDate();
        }
        if (raw instanceof java.util.Date utilDate) {
            return utilDate.toInstant().atZone(EAST_AFRICA).toLocalDate();
        }
        if (raw instanceof CharSequence text) {
            return LocalDate.parse(text.toString());
        }
        throw new IllegalArgumentException("Expected date JDBC type, got " + raw.getClass());
    }

    private record DayAggregate(BigDecimal revenueTzs, BigDecimal tonnes, long orderCount) {
        private static DayAggregate zero() {
            return new DayAggregate(BigDecimal.ZERO, BigDecimal.ZERO, 0L);
        }
    }

    private static ProductDistributionV2TrendsDTO buildProductPeriodTrends(
            ProductDistributionV2RowDTO current,
            ProductDistributionV2RowDTO previous) {
        ProductDistributionV2TrendsDTO trends = new ProductDistributionV2TrendsDTO();
        trends.setTrendLabel(LABEL_PREVIOUS_PERIOD);
        if (previous == null) {
            trends.setUnitsSoldTrendPercentage(
                    SupplierPortalDashboardMath.percentChangeLongOrZero(current.getUnitsSold(), 0L));
            return trends;
        }
        trends.setUnitsSoldTrendPercentage(
                SupplierPortalDashboardMath.percentChangeLongOrZero(
                        current.getUnitsSold(), previous.getUnitsSold()));
        return trends;
    }

    private static DistrictPerformanceV2RowDTO mapDistrictRow(Object[] r) {
        DistrictPerformanceV2RowDTO dto = new DistrictPerformanceV2RowDTO();
        dto.setOrganizationLocationPublicId(coerceUuid(r[0]));
        dto.setOrganizationLocationName(r[1] != null ? r[1].toString() : null);
        dto.setSupplierLineRevenueTzs(nzBdStatic(coerceToBigDecimal(r[2])));
        dto.setSettledTonnes(nzBdStatic(coerceToBigDecimal(r[3])));
        dto.setDistinctCustomerCount(coerceLong(r[4]));
        dto.setDeliveredOrderCount(coerceLong(r[5]));
        return dto;
    }

    /**
     * Stable merge key for district-performance rows (region id in district mode, ward id in ward mode).
     * {@code organizationLocationPublicId} is null for the UNKNOWN bucket; fall back to display name.
     */
    static String districtMergeKey(DistrictPerformanceV2RowDTO row) {
        if (row.getOrganizationLocationPublicId() != null) {
            return row.getOrganizationLocationPublicId().toString();
        }
        String name = row.getOrganizationLocationName();
        return "UNKNOWN:" + (name != null ? name : "");
    }

    private static DistrictPerformanceV2TrendsDTO buildDistrictPeriodTrends(
            DistrictPerformanceV2RowDTO current,
            DistrictPerformanceV2RowDTO previous) {
        DistrictPerformanceV2TrendsDTO trends = new DistrictPerformanceV2TrendsDTO();
        trends.setTrendLabel(LABEL_PREVIOUS_PERIOD);
        if (previous == null) {
            trends.setSupplierLineRevenueTzsTrendPercentage(
                    SupplierPortalDashboardMath.percentChangeOrZero(current.getSupplierLineRevenueTzs(), BigDecimal.ZERO));
            trends.setSettledTonnesTrendPercentage(
                    SupplierPortalDashboardMath.percentChangeOrZero(current.getSettledTonnes(), BigDecimal.ZERO));
            trends.setDistinctCustomerCountTrendPercentage(
                    SupplierPortalDashboardMath.percentChangeLongOrZero(current.getDistinctCustomerCount(), 0L));
            trends.setDeliveredOrderCountTrendPercentage(
                    SupplierPortalDashboardMath.percentChangeLongOrZero(current.getDeliveredOrderCount(), 0L));
            return trends;
        }
        trends.setSupplierLineRevenueTzsTrendPercentage(
                SupplierPortalDashboardMath.percentChangeOrZero(
                        current.getSupplierLineRevenueTzs(), previous.getSupplierLineRevenueTzs()));
        trends.setSettledTonnesTrendPercentage(
                SupplierPortalDashboardMath.percentChangeOrZero(
                        current.getSettledTonnes(), previous.getSettledTonnes()));
        trends.setDistinctCustomerCountTrendPercentage(
                SupplierPortalDashboardMath.percentChangeLongOrZero(
                        current.getDistinctCustomerCount(), previous.getDistinctCustomerCount()));
        trends.setDeliveredOrderCountTrendPercentage(
                SupplierPortalDashboardMath.percentChangeLongOrZero(
                        current.getDeliveredOrderCount(), previous.getDeliveredOrderCount()));
        return trends;
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

    private static long coerceLong(Object raw) {
        if (raw == null) {
            return 0L;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        throw new IllegalArgumentException("Expected numeric JDBC type for sums/counts, got " + raw.getClass());
    }

    /**
     * {@link #nzBd(BigDecimal)} counterpart usable from static mappers without an instance receiver.
     */
    private static BigDecimal nzBdStatic(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<SalesRevenueDTO> buildDailyRevenueTrendSeries(UUID supplierId,
                                                             SupplierPortalGeographyFilter geo,
                                                             SupplierPortalTrendPeriod period) {
        List<Object[]> raw = salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                supplierId,
                geo.regionIdsParam(),
                geo.regionFilterDisabledParam(),
                geo.locationIdsParam(),
                geo.locationFilterDisabledParam(),
                dateStartInclusive(period.startInclusive()),
                dateEndInclusive(period.endInclusive()));

        Map<LocalDate, BigDecimal> revenueByDay = new HashMap<>();
        for (Object[] row : raw) {
            BigDecimal cogs = nzBd(coerceToBigDecimal(row[0]));
            LocalDate day = coerceLocalDate(row[1]);
            revenueByDay.put(day, cogs);
        }

        List<SalesRevenueDTO> series = new ArrayList<>();
        for (LocalDate day = period.startInclusive(); !day.isAfter(period.endInclusive()); day = day.plusDays(1)) {
            BigDecimal total = revenueByDay.getOrDefault(day, BigDecimal.ZERO);
            SalesRevenueDTO dto = new SalesRevenueDTO();
            dto.setTotalRevenue(total);
            dto.setYear(day.getYear());
            dto.setMonth(day.getMonthValue());
            dto.setQuarter(null);
            dto.setDay(day.toString());
            dto.setLabel(day.toString());
            series.add(dto);
        }
        return series;
    }

    private List<SalesRevenueDTO> zeroFillMonthlyRevenueSeries(int year, List<Object[]> rows) {
        Map<Integer, BigDecimal> revenueByMonth = new HashMap<>();
        for (Object[] row : rows) {
            revenueByMonth.put(intFromJdbcNumber(row[2]), nzBd(coerceToBigDecimal(row[0])));
        }

        List<SalesRevenueDTO> series = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            BigDecimal total = revenueByMonth.getOrDefault(month, BigDecimal.ZERO);
            SalesRevenueDTO dto = new SalesRevenueDTO();
            dto.setTotalRevenue(total);
            dto.setYear(year);
            dto.setMonth(month);
            dto.setQuarter(null);
            dto.setDay(null);
            dto.setLabel(Month.of(month).name() + " " + year);
            series.add(dto);
        }
        return series;
    }

    private TrendMetricDTO<BigDecimal> buildRevenueTrendHeadlineForCalendarMonth(UUID supplierId,
                                                                                 SupplierPortalGeographyFilter geo,
                                                                                 SupplierPortalTrendPeriod period,
                                                                                 List<SalesRevenueDTO> series) {
        BigDecimal currentTotal = series.stream()
                .map(SalesRevenueDTO::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        YearMonth previousMonth = SupplierPortalTrendPeriodValidation.previousCalendarMonth(
                YearMonth.of(period.year(), period.month()));
        BigDecimal baseline = nzBd(sumSupplierCogs(
                supplierId,
                geo,
                dateStartInclusive(previousMonth.atDay(1)),
                dateEndInclusive(previousMonth.atEndOfMonth())));

        return new TrendMetricDTO<>(
                currentTotal,
                SupplierPortalDashboardMath.percentChange(currentTotal, baseline),
                LABEL_PREVIOUS_PERIOD);
    }

    private TrendMetricDTO<BigDecimal> buildRevenueTrendHeadlineForCalendarYear(UUID supplierId,
                                                                                SupplierPortalGeographyFilter geo,
                                                                                SupplierPortalTrendPeriod period,
                                                                                List<SalesRevenueDTO> series) {
        BigDecimal currentTotal = series.stream()
                .map(SalesRevenueDTO::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate[] previousYear = SupplierPortalTrendPeriodValidation.previousCalendarYearWindow(period.year());
        BigDecimal baseline = nzBd(sumSupplierCogs(
                supplierId,
                geo,
                dateStartInclusive(previousYear[0]),
                dateEndInclusive(previousYear[1])));

        return new TrendMetricDTO<>(
                currentTotal,
                SupplierPortalDashboardMath.percentChange(currentTotal, baseline),
                LABEL_VS_LAST_YEAR);
    }

    private List<Object[]> loadMonthBuckets(UUID supplierId,
                                            SupplierPortalGeographyFilter geo,
                                            LocalDate bucketWindowStartInclusive,
                                            LocalDate bucketWindowEndInclusive) {
        return salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
                supplierId,
                geo.regionIdsParam(),
                geo.regionFilterDisabledParam(),
                geo.locationIdsParam(),
                geo.locationFilterDisabledParam(),
                dateStartInclusive(bucketWindowStartInclusive),
                dateEndInclusive(bucketWindowEndInclusive));
    }

    /**
     * Soft resolve for dashboard reads: missing supplier/user association returns empty analytics
     * instead of 400 so empty local DBs and non-supplier accounts still render the UI.
     */
    private Optional<Suppliers> findAssociatedSupplier(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return Optional.empty();
        }
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(principal.getName());
        if (personOptional.isEmpty()) {
            log.debug("Supplier portal dashboard: user not found for {}", principal.getName());
            return Optional.empty();
        }
        String supplierPublicId = Optional.ofNullable(personOptional.get().getSupplierPublicId())
                .filter(id -> !id.isEmpty())
                .orElse(null);
        if (supplierPublicId == null) {
            log.debug("Supplier portal dashboard: no associated supplier for {}", principal.getName());
            return Optional.empty();
        }
        try {
            return supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(supplierPublicId));
        } catch (IllegalArgumentException ex) {
            log.warn("Supplier portal dashboard: invalid supplierPublicId {} for {}",
                    supplierPublicId, principal.getName());
            return Optional.empty();
        }
    }

    private static SummaryCardsV2DTO emptySummaryCards() {
        SummaryCardsV2DTO dto = new SummaryCardsV2DTO();
        dto.setActiveCustomers(new TrendMetricDTO<>(0L, null, LABEL_PREVIOUS_PERIOD));
        dto.setTotalOrders(new TrendMetricDTO<>(0L, null, LABEL_PREVIOUS_PERIOD));
        dto.setTotalQuantitySoldTonnes(new TrendMetricDTO<>(BigDecimal.ZERO, null, LABEL_PREVIOUS_PERIOD));
        dto.setTotalCogsTzs(new TrendMetricDTO<>(BigDecimal.ZERO, null, LABEL_PREVIOUS_PERIOD));
        return dto;
    }

    private SalesTrendV2DTO emptySalesTrend(SupplierPortalTrendPeriod period) {
        List<SalesRevenueDTO> series;
        if (period.granularity() == SupplierPortalTrendGranularity.DAILY_IN_CALENDAR_MONTH) {
            series = new ArrayList<>();
            for (LocalDate day = period.startInclusive(); !day.isAfter(period.endInclusive()); day = day.plusDays(1)) {
                SalesRevenueDTO point = new SalesRevenueDTO();
                point.setTotalRevenue(BigDecimal.ZERO);
                point.setYear(day.getYear());
                point.setMonth(day.getMonthValue());
                point.setQuarter(null);
                point.setDay(day.toString());
                point.setLabel(day.toString());
                series.add(point);
            }
        } else {
            series = zeroFillMonthlyRevenueSeries(period.year(), List.of());
        }
        SalesTrendV2DTO dto = new SalesTrendV2DTO();
        dto.setSeries(series);
        dto.setMonthToDateCogsVersusLastYear(new TrendMetricDTO<>(
                BigDecimal.ZERO, null, LABEL_PREVIOUS_PERIOD));
        return dto;
    }

    private SalesCalendarV2DTO emptySalesCalendar(YearMonth selectedMonth) {
        LocalDate monthStart = selectedMonth.atDay(1);
        LocalDate monthEnd = selectedMonth.atEndOfMonth();
        List<SalesCalendarDayV2DTO> days = new ArrayList<>(selectedMonth.lengthOfMonth());
        for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
            days.add(toSalesCalendarDay(date, DayAggregate.zero(), BigDecimal.ZERO));
        }

        SalesCalendarSummaryV2DTO summary = new SalesCalendarSummaryV2DTO();
        summary.setPeakDay(null);
        summary.setPeakDayRevenueTzs(BigDecimal.ZERO);
        summary.setPeakDayRevenueLabel(SupplierPortalDashboardMath.formatTzsLabel(BigDecimal.ZERO));
        summary.setUnitsSoldTonnes(BigDecimal.ZERO);
        summary.setUnitsSoldLabel(SupplierPortalDashboardMath.formatTonnesLabel(BigDecimal.ZERO));
        summary.setTotalOrders(0L);
        summary.setMonthlyRevenueVersusPreviousMonth(new TrendMetricDTO<>(
                BigDecimal.ZERO, null, LABEL_VS_PREVIOUS_MONTH));
        summary.setMonthlyOrdersVersusPreviousMonth(new TrendMetricDTO<>(
                0L, null, LABEL_VS_PREVIOUS_MONTH));
        summary.setMonthlyUnitsSoldVersusPreviousMonth(new TrendMetricDTO<>(
                BigDecimal.ZERO, null, LABEL_VS_PREVIOUS_MONTH));

        SalesCalendarV2DTO dto = new SalesCalendarV2DTO();
        dto.setMonth(selectedMonth.getMonthValue());
        dto.setYear(selectedMonth.getYear());
        dto.setMonthLabel(selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        dto.setSummary(summary);
        dto.setDays(days);
        return dto;
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
        throw new IllegalArgumentException("Expected numeric JDBC type for revenue aggregate, got " + raw.getClass());
    }

    private static int intFromJdbcNumber(Object raw) {
        if (raw == null) {
            return 0;
        }
        if (raw instanceof Number n) {
            return n.intValue();
        }
        throw new IllegalArgumentException("Expected numeric JDBC type for year/month, got " + raw.getClass());
    }

    private BigDecimal sumSupplierCogs(UUID supplierId, SupplierPortalGeographyFilter geo,
                                       LocalDateTime from, LocalDateTime to) {
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();
        return salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to);
    }

    private List<UUID> optionalUuidList(UUID id) {
        return id == null ? List.of() : List.of(id);
    }
}
