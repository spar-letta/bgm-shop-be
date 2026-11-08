package com.app.itibari.services;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.report.CustomerAnalysisActivityCalendarDayV2DTO;
import com.app.itibari.models.report.CustomerAnalysisActivityCalendarPeakDayV2DTO;
import com.app.itibari.models.report.CustomerAnalysisActivityCalendarSummaryV2DTO;
import com.app.itibari.models.report.CustomerAnalysisActivityCalendarV2DTO;
import com.app.itibari.models.report.CustomerAnalysisCustomerRowV2DTO;
import com.app.itibari.models.report.CustomerAnalysisDailyOrderPointDTO;
import com.app.itibari.models.report.CustomerAnalysisDistrictCogsRowV2DTO;
import com.app.itibari.models.report.CustomerAnalysisDistrictCogsV2DTO;
import com.app.itibari.models.report.CustomerAnalysisDistrictRankingRowV2DTO;
import com.app.itibari.models.report.CustomerAnalysisDistrictRankingV2DTO;
import com.app.itibari.models.report.CustomerAnalysisOrdersTrendV2DTO;
import com.app.itibari.models.report.CustomerAnalysisSummaryMetricsV2DTO;
import com.app.itibari.models.report.TrendMetricDTO;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.services.supplierportal.SupplierPortalCustomerAnalysisValidation;
import com.app.itibari.services.supplierportal.SupplierPortalDashboardMath;
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
import java.math.RoundingMode;
import java.security.Principal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Supplier Portal Customer Analysis V2 — additive analytics bypassing legacy customer list/report contracts.
 *
 * <p>Summary KPI trends match {@link SupplierPortalDashboardServiceImpl#getDashboardSummaryMetrics}:
 * {@link SupplierPortalDashboardMath#percentChangeLongOrZero} / {@link SupplierPortalDashboardMath#percentChangeOrZero}
 * so {@code trendPercentage} is never null.</p>
 *
 * <p>Orders-trend headline matches Product Analysis sales-trend:
 * {@link SupplierPortalDashboardMath#percentChange} (null when previous baseline is zero).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierPortalCustomerAnalysisServiceImpl implements SupplierPortalCustomerAnalysisService {

    private static final ZoneId EAST_AFRICA = ZoneId.of("Africa/Dar_es_Salaam");
    private static final String LABEL_PREVIOUS_PERIOD = "vs Previous Period";
    private static final String LABEL_VS_LAST_YEAR = "vs Last Year";

    private final SalesOrdersRepository salesOrdersRepository;
    private final PersonRepository personRepository;
    private final SupplierRepository supplierRepository;

    @Override
    public CustomerAnalysisSummaryMetricsV2DTO getSummaryMetrics(List<UUID> regionIds,
                                                                 List<UUID> locationIds,
                                                                 List<UUID> productIds,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate,
                                                                 Principal principal) {
        SupplierPortalCustomerAnalysisValidation.validateDateRange(startDate, endDate);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.of(productIds);

        LocalDateTime currentStart = dateStartInclusive(startDate);
        LocalDateTime currentEnd = dateEndInclusive(endDate);
        LocalDate[] prev = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousStart = dateStartInclusive(prev[0]);
        LocalDateTime previousEnd = dateEndInclusive(prev[1]);

        UUID supplierId = supplier.getPublicId();

        long customersCurrent = nz(countDistinctDeliveredCustomers(supplierId, geo, products, currentStart, currentEnd));
        long customersPrevious = nz(countDistinctDeliveredCustomers(supplierId, geo, products, previousStart, previousEnd));

        long newCustomersCurrent = nz(salesOrdersRepository.countNewDeliveredCustomersForCustomerAnalysis(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), currentStart, currentEnd));
        long newCustomersPrevious = nz(salesOrdersRepository.countNewDeliveredCustomersForCustomerAnalysis(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), previousStart, previousEnd));

        BigDecimal aovCurrent = computeAov(supplierId, geo, products, currentStart, currentEnd);
        BigDecimal aovPrevious = computeAov(supplierId, geo, products, previousStart, previousEnd);

        long frequentCustomersCurrent = nz(salesOrdersRepository.countFrequentDeliveredCustomersForCustomerAnalysis(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), currentStart, currentEnd));
        long frequentCustomersPrevious = nz(salesOrdersRepository.countFrequentDeliveredCustomersForCustomerAnalysis(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), previousStart, previousEnd));

        CustomerAnalysisSummaryMetricsV2DTO dto = new CustomerAnalysisSummaryMetricsV2DTO();
        dto.setCustomers(new TrendMetricDTO<>(
                customersCurrent,
                SupplierPortalDashboardMath.percentChangeLongOrZero(customersCurrent, customersPrevious),
                LABEL_PREVIOUS_PERIOD));
        dto.setNewCustomers(new TrendMetricDTO<>(
                newCustomersCurrent,
                SupplierPortalDashboardMath.percentChangeLongOrZero(newCustomersCurrent, newCustomersPrevious),
                LABEL_PREVIOUS_PERIOD));
        dto.setAov(new TrendMetricDTO<>(
                aovCurrent,
                SupplierPortalDashboardMath.percentChangeOrZero(aovCurrent, aovPrevious),
                LABEL_PREVIOUS_PERIOD));
        dto.setFrequentCustomers(new TrendMetricDTO<>(
                frequentCustomersCurrent,
                SupplierPortalDashboardMath.percentChangeLongOrZero(
                        frequentCustomersCurrent, frequentCustomersPrevious),
                LABEL_PREVIOUS_PERIOD));
        return dto;
    }

    public CustomerAnalysisSummaryMetricsV2DTO getSummaryMetrics(UUID regionId,
                                                                 UUID locationId,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate,
                                                                 Principal principal) {
        return getSummaryMetrics(optionalUuidList(regionId), optionalUuidList(locationId), List.of(),
                startDate, endDate, principal);
    }

    @Override
    public Page<CustomerAnalysisCustomerRowV2DTO> getCustomers(List<UUID> regionIds,
                                                               List<UUID> locationIds,
                                                               List<UUID> productIds,
                                                               LocalDate startDate,
                                                               LocalDate endDate,
                                                               String search,
                                                               Principal principal,
                                                               Pageable pageable) {
        SupplierPortalCustomerAnalysisValidation.validateDateRange(startDate, endDate);
        SupplierPortalCustomerAnalysisValidation.validateCustomersPageable(pageable);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.of(productIds);

        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        String searchPattern = normalizeSearchPattern(search);

        Page<Object[]> raw = salesOrdersRepository.pageCustomerAnalysisCustomersV2(
                supplier.getPublicId(),
                geo.regionIdsParam(),
                geo.regionFilterDisabledParam(),
                geo.locationIdsParam(),
                geo.locationFilterDisabledParam(),
                products.productIdsParam(),
                products.productFilterDisabledParam(),
                dateStartInclusive(startDate),
                dateEndInclusive(endDate),
                searchPattern,
                sanitized);

        List<CustomerAnalysisCustomerRowV2DTO> mapped = new ArrayList<>(raw.getContent().size());
        for (Object[] row : raw.getContent()) {
            mapped.add(mapCustomerRow(row));
        }
        return new PageImpl<>(mapped, sanitized, raw.getTotalElements());
    }

    public Page<CustomerAnalysisCustomerRowV2DTO> getCustomers(UUID regionId,
                                                               UUID locationId,
                                                               LocalDate startDate,
                                                               LocalDate endDate,
                                                               String search,
                                                               Principal principal,
                                                               Pageable pageable) {
        return getCustomers(optionalUuidList(regionId), optionalUuidList(locationId), List.of(),
                startDate, endDate, search, principal, pageable);
    }

    @Override
    public Page<CustomerAnalysisCustomerRowV2DTO> getCustomersForExport(UUID regionId,
                                                                        UUID locationId,
                                                                        List<UUID> productIds,
                                                                        LocalDate startDate,
                                                                        LocalDate endDate,
                                                                        String search,
                                                                        Principal principal,
                                                                        Pageable pageable) {
        SupplierPortalCustomerAnalysisValidation.validateDateRange(startDate, endDate);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.of(productIds);

        // Uncapped: return every matching product/order for the filter window (no EXPORT_MAX / LIMIT).
        String searchPattern = normalizeSearchPattern(search);
        LocalDateTime from = dateStartInclusive(startDate);
        LocalDateTime to = dateEndInclusive(endDate);

        List<Object[]> raw = salesOrdersRepository.listCustomerAnalysisCustomersForExportV2(
                supplier.getPublicId(),
                regionId,
                locationId,
                products.productIdsParam(),
                products.productFilterDisabledParam(),
                from,
                to,
                searchPattern);

        List<CustomerAnalysisCustomerRowV2DTO> mapped = new ArrayList<>(raw.size());
        for (Object[] row : raw) {
            mapped.add(mapCustomerRow(row));
        }
        return new PageImpl<>(mapped, PageRequest.of(0, Math.max(mapped.size(), 1)), mapped.size());
    }

    @Override
    public CustomerAnalysisDistrictRankingV2DTO getDistrictRanking(List<UUID> regionIds,
                                                                   List<UUID> locationIds,
                                                                   List<UUID> productIds,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate,
                                                                   Integer limit,
                                                                   Principal principal) {
        SupplierPortalCustomerAnalysisValidation.validateDateRange(startDate, endDate);
        int effectiveLimit = SupplierPortalCustomerAnalysisValidation.effectiveDistrictRankingLimit(limit);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.of(productIds);

        LocalDateTime from = dateStartInclusive(startDate);
        LocalDateTime to = dateEndInclusive(endDate);
        List<Object[]> raw = geo.usesWardRankingBucket()
                ? salesOrdersRepository.listCustomerAnalysisDistrictRankingV2(
                        supplier.getPublicId(), geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                        products.productIdsParam(), products.productFilterDisabledParam(), from, to)
                : salesOrdersRepository.listCustomerAnalysisRegionRankingV2(
                        supplier.getPublicId(), products.productIdsParam(), products.productFilterDisabledParam(),
                        from, to);

        List<CustomerAnalysisDistrictRankingRowV2DTO> rows = new ArrayList<>();
        int rank = 0;
        for (Object[] row : raw) {
            if (++rank > effectiveLimit) {
                break;
            }
            CustomerAnalysisDistrictRankingRowV2DTO dto = new CustomerAnalysisDistrictRankingRowV2DTO();
            dto.setRank(rank);
            dto.setOrganizationLocationPublicId(coerceUuid(row[0]));
            dto.setOrganizationLocationName(row[1] != null ? row[1].toString() : "UNKNOWN");
            dto.setDistinctCustomerCount(coerceLong(row[2]));
            dto.setDeliveredOrderCount(coerceLong(row[3]));
            dto.setAverageOrderValueTzs(computeAverageOrderValueTzs(
                    nzBd(coerceToBigDecimal(row[4])), coerceLong(row[3])));
            rows.add(dto);
        }

        applyAverageOrderValuePercentageOfTotal(rows);

        CustomerAnalysisDistrictRankingV2DTO response = new CustomerAnalysisDistrictRankingV2DTO();
        response.setRows(rows);
        response.setTotalAverageOrderValueTzs(computeAov(supplier.getPublicId(), geo, products, from, to));
        return response;
    }

    public CustomerAnalysisDistrictRankingV2DTO getDistrictRanking(UUID regionId,
                                                                   UUID locationId,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate,
                                                                   Integer limit,
                                                                   Principal principal) {
        return getDistrictRanking(optionalUuidList(regionId), optionalUuidList(locationId), List.of(),
                startDate, endDate, limit, principal);
    }

    @Override
    public CustomerAnalysisDistrictCogsV2DTO getDistrictCogs(List<UUID> regionIds,
                                                             List<UUID> locationIds,
                                                             List<UUID> productIds,
                                                             LocalDate startDate,
                                                             LocalDate endDate,
                                                             Integer limit,
                                                             Principal principal) {
        SupplierPortalCustomerAnalysisValidation.validateDateRange(startDate, endDate);
        int effectiveLimit = SupplierPortalCustomerAnalysisValidation.effectiveDistrictRankingLimit(limit);
        Suppliers supplier = resolveSupplier(principal);

        LocalDateTime from = dateStartInclusive(startDate);
        LocalDateTime to = dateEndInclusive(endDate);
        UUID supplierId = supplier.getPublicId();
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.of(productIds);

        List<Object[]> raw = geo.usesWardRankingBucket()
                ? salesOrdersRepository.listSupplierPortalAllProductsWardCogsByTotalCogs(
                        supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                        geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                        products.productIdsParam(), products.productFilterDisabledParam(), from, to)
                : salesOrdersRepository.listCustomerAnalysisRegionCogsV2(
                        supplierId, products.productIdsParam(), products.productFilterDisabledParam(), from, to);

        BigDecimal totalCogsTzs = nzBd(sumSupplierCogs(supplierId, geo, products, from, to));

        List<CustomerAnalysisDistrictCogsRowV2DTO> rows = new ArrayList<>();
        int rowCount = 0;
        for (Object[] row : raw) {
            if (++rowCount > effectiveLimit) {
                break;
            }
            CustomerAnalysisDistrictCogsRowV2DTO dto = new CustomerAnalysisDistrictCogsRowV2DTO();
            dto.setOrganizationLocationPublicId(coerceUuid(row[0]));
            dto.setOrganizationLocationName(row[1] != null ? row[1].toString() : "UNKNOWN");
            dto.setTotalCogsTzs(nzBd(coerceToBigDecimal(row[2])));
            rows.add(dto);
        }

        CustomerAnalysisDistrictCogsV2DTO response = new CustomerAnalysisDistrictCogsV2DTO();
        response.setRows(rows);
        response.setTotalCogsTzs(totalCogsTzs);
        return response;
    }

    public CustomerAnalysisDistrictCogsV2DTO getDistrictCogs(UUID regionId,
                                                             UUID locationId,
                                                             LocalDate startDate,
                                                             LocalDate endDate,
                                                             Integer limit,
                                                             Principal principal) {
        return getDistrictCogs(optionalUuidList(regionId), optionalUuidList(locationId), List.of(),
                startDate, endDate, limit, principal);
    }

    /**
     * Daily and headline order counts use {@link SalesOrdersRepository#aggregateCustomerAnalysisActivityByDay}
     * (distinct {@code so.public_id} per day, DELIVERED + {@code quantity_delivered > 0}), including optional product filter.
     */
    @Override
    public CustomerAnalysisOrdersTrendV2DTO getOrdersTrend(List<UUID> regionIds,
                                                          List<UUID> locationIds,
                                                          List<UUID> productIds,
                                                          Integer month,
                                                          Integer year,
                                                          Principal principal) {
        SupplierPortalTrendPeriod period = SupplierPortalCustomerAnalysisValidation.resolveOrdersTrendPeriod(
                month, year, LocalDate.now(EAST_AFRICA));
        Suppliers supplier = resolveSupplier(principal);
        return buildOrdersTrend(
                supplier.getPublicId(),
                SupplierPortalGeographyFilter.of(regionIds, locationIds),
                SupplierPortalProductFilter.of(productIds),
                period);
    }

    public CustomerAnalysisOrdersTrendV2DTO getOrdersTrend(UUID regionId,
                                                           UUID locationId,
                                                           Integer month,
                                                           Integer year,
                                                           Principal principal) {
        return getOrdersTrend(optionalUuidList(regionId), optionalUuidList(locationId), List.of(), month, year, principal);
    }

    private CustomerAnalysisOrdersTrendV2DTO buildOrdersTrend(UUID supplierId,
                                                              SupplierPortalGeographyFilter geo,
                                                              SupplierPortalProductFilter products,
                                                              SupplierPortalTrendPeriod period) {
        if (period.granularity() == SupplierPortalTrendGranularity.DAILY_IN_CALENDAR_MONTH) {
            LocalDateTime from = dateStartInclusive(period.startInclusive());
            LocalDateTime to = dateEndInclusive(period.endInclusive());
            List<Object[]> raw = salesOrdersRepository.aggregateCustomerAnalysisActivityByDay(
                    supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                    geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                    products.productIdsParam(), products.productFilterDisabledParam(), from, to);

            Map<LocalDate, Long> ordersByDay = new HashMap<>();
            for (Object[] row : raw) {
                ordersByDay.put(coerceSqlDate(row[0]), coerceLong(row[2]));
            }

            List<CustomerAnalysisDailyOrderPointDTO> series = new ArrayList<>();
            long currentTotal = 0L;
            for (LocalDate day = period.startInclusive(); !day.isAfter(period.endInclusive()); day = day.plusDays(1)) {
                long dayCount = ordersByDay.getOrDefault(day, 0L);
                series.add(new CustomerAnalysisDailyOrderPointDTO(day, dayCount));
                currentTotal += dayCount;
            }

            YearMonth previousMonth = SupplierPortalTrendPeriodValidation.previousCalendarMonth(
                    YearMonth.of(period.year(), period.month()));
            long previousTotal = sumDistinctDeliveredOrdersFromActivityByDay(
                    supplierId, geo, products,
                    dateStartInclusive(previousMonth.atDay(1)),
                    dateEndInclusive(previousMonth.atEndOfMonth()));

            return buildOrdersTrendDto(series, currentTotal, previousTotal, LABEL_PREVIOUS_PERIOD);
        }

        List<Object[]> raw = salesOrdersRepository.aggregateSupplierPortalDeliveredOrdersByCreationMonth(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(),
                dateStartInclusive(period.startInclusive()),
                dateEndInclusive(period.endInclusive()));
        List<CustomerAnalysisDailyOrderPointDTO> series = buildMonthlyOrdersTrendSeries(period.year(), raw);
        long currentTotal = series.stream().mapToLong(CustomerAnalysisDailyOrderPointDTO::getDeliveredOrderCount).sum();

        LocalDate[] prevYear = SupplierPortalTrendPeriodValidation.previousCalendarYearWindow(period.year());
        long previousTotal = sumDistinctDeliveredOrdersFromMonthlyAggregate(
                supplierId, geo, products,
                dateStartInclusive(prevYear[0]),
                dateEndInclusive(prevYear[1]));

        return buildOrdersTrendDto(series, currentTotal, previousTotal, LABEL_VS_LAST_YEAR);
    }

    private List<CustomerAnalysisDailyOrderPointDTO> buildMonthlyOrdersTrendSeries(int year, List<Object[]> raw) {
        Map<Integer, Long> ordersByMonth = new HashMap<>();
        for (Object[] row : raw) {
            int month = intFromJdbcNumber(row[1]);
            ordersByMonth.put(month, coerceLong(row[2]));
        }

        List<CustomerAnalysisDailyOrderPointDTO> series = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            series.add(new CustomerAnalysisDailyOrderPointDTO(
                    LocalDate.of(year, month, 1),
                    ordersByMonth.getOrDefault(month, 0L)));
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

    private long sumDistinctDeliveredOrdersFromMonthlyAggregate(UUID supplierId,
                                                                SupplierPortalGeographyFilter geo,
                                                                SupplierPortalProductFilter products,
                                                                LocalDateTime from,
                                                                LocalDateTime to) {
        List<Object[]> raw = salesOrdersRepository.aggregateSupplierPortalDeliveredOrdersByCreationMonth(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to);
        long total = 0L;
        for (Object[] row : raw) {
            total += coerceLong(row[2]);
        }
        return total;
    }

    private CustomerAnalysisOrdersTrendV2DTO buildOrdersTrendDto(List<CustomerAnalysisDailyOrderPointDTO> series,
                                                                 long currentTotal,
                                                                 long previousTotal,
                                                                 String trendLabel) {
        CustomerAnalysisOrdersTrendV2DTO dto = new CustomerAnalysisOrdersTrendV2DTO();
        dto.setSeries(series);
        dto.setHeadline(new TrendMetricDTO<>(
                currentTotal,
                SupplierPortalDashboardMath.percentChangeLong(currentTotal, previousTotal),
                trendLabel));
        return dto;
    }

    @Override
    public CustomerAnalysisActivityCalendarV2DTO getActivityCalendar(List<UUID> regionIds,
                                                                    List<UUID> locationIds,
                                                                    List<UUID> productIds,
                                                                    Integer month,
                                                                    Integer year,
                                                                    Principal principal) {
        SupplierPortalCustomerAnalysisValidation.validateActivityCalendarMonthYear(month, year);
        Suppliers supplier = resolveSupplier(principal);
        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of(regionIds, locationIds);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.of(productIds);

        YearMonth calendarMonth = YearMonth.of(year, month);
        LocalDate startDate = calendarMonth.atDay(1);
        LocalDate endDate = calendarMonth.atEndOfMonth();
        LocalDateTime from = dateStartInclusive(startDate);
        LocalDateTime to = dateEndInclusive(endDate);

        List<Object[]> raw = salesOrdersRepository.aggregateCustomerAnalysisActivityByDay(
                supplier.getPublicId(), geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to);

        Map<LocalDate, long[]> byDay = new HashMap<>();
        for (Object[] row : raw) {
            byDay.put(coerceSqlDate(row[0]), new long[]{coerceLong(row[1]), coerceLong(row[2])});
        }

        long monthPeakDistinctCustomers = byDay.values().stream()
                .mapToLong(values -> values[0])
                .max()
                .orElse(0L);
        BigDecimal monthPeak = BigDecimal.valueOf(monthPeakDistinctCustomers);

        LocalDate peakDate = null;
        long peakDistinctCustomers = 0L;
        long peakDeliveredOrders = 0L;
        long totalOrdersCurrent = 0L;

        List<CustomerAnalysisActivityCalendarDayV2DTO> days = new ArrayList<>(calendarMonth.lengthOfMonth());
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            long[] values = byDay.getOrDefault(day, new long[]{0L, 0L});
            long distinctCustomers = values[0];
            long deliveredOrders = values[1];
            totalOrdersCurrent += deliveredOrders;

            if (distinctCustomers > peakDistinctCustomers) {
                peakDistinctCustomers = distinctCustomers;
                peakDeliveredOrders = deliveredOrders;
                peakDate = day;
            } else if (distinctCustomers == peakDistinctCustomers && distinctCustomers > 0L
                    && (peakDate == null || day.isBefore(peakDate))) {
                peakDeliveredOrders = deliveredOrders;
                peakDate = day;
            }

            CustomerAnalysisActivityCalendarDayV2DTO dayDto = new CustomerAnalysisActivityCalendarDayV2DTO();
            dayDto.setDate(day);
            dayDto.setDistinctCustomerCount(distinctCustomers);
            dayDto.setDeliveredOrderCount(deliveredOrders);
            double intensity = SupplierPortalDashboardMath.intensityRatio(
                    BigDecimal.valueOf(distinctCustomers), monthPeak);
            dayDto.setIntensity(intensity);
            dayDto.setIntensityLevel(SupplierPortalDashboardMath.intensityLevel(intensity));
            days.add(dayDto);
        }

        UUID supplierId = supplier.getPublicId();
        LocalDate[] previousWindow = SupplierPortalDashboardMath.inclusivePreviousWindow(startDate, endDate);
        LocalDateTime previousFrom = dateStartInclusive(previousWindow[0]);
        LocalDateTime previousTo = dateEndInclusive(previousWindow[1]);

        long totalOrdersPrevious = sumDistinctDeliveredOrdersFromActivityByDay(
                supplierId, geo, products, previousFrom, previousTo);
        BigDecimal unitsSoldCurrent = nzBd(sumAllProductsSettledTonnes(supplierId, geo, products, from, to));
        BigDecimal unitsSoldPrevious = nzBd(sumAllProductsSettledTonnes(supplierId, geo, products, previousFrom, previousTo));

        CustomerAnalysisActivityCalendarSummaryV2DTO summary = new CustomerAnalysisActivityCalendarSummaryV2DTO();
        if (peakDate != null) {
            summary.setPeakDay(new CustomerAnalysisActivityCalendarPeakDayV2DTO(
                    peakDate, peakDistinctCustomers, peakDeliveredOrders));
        }
        summary.setTotalOrders(new TrendMetricDTO<>(
                totalOrdersCurrent,
                SupplierPortalDashboardMath.percentChangeLongOrZero(totalOrdersCurrent, totalOrdersPrevious),
                LABEL_PREVIOUS_PERIOD));
        summary.setUnitsSold(new TrendMetricDTO<>(
                unitsSoldCurrent,
                SupplierPortalDashboardMath.percentChangeOrZero(unitsSoldCurrent, unitsSoldPrevious),
                LABEL_PREVIOUS_PERIOD));

        CustomerAnalysisActivityCalendarV2DTO dto = new CustomerAnalysisActivityCalendarV2DTO();
        dto.setMonth(month);
        dto.setYear(year);
        dto.setMonthLabel(calendarMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        dto.setDays(days);
        dto.setSummary(summary);
        return dto;
    }

    public CustomerAnalysisActivityCalendarV2DTO getActivityCalendar(UUID regionId,
                                                                     UUID locationId,
                                                                     Integer month,
                                                                     Integer year,
                                                                     Principal principal) {
        return getActivityCalendar(optionalUuidList(regionId), optionalUuidList(locationId), List.of(), month, year, principal);
    }

    private BigDecimal computeAov(UUID supplierId,
                                  SupplierPortalGeographyFilter geo,
                                  SupplierPortalProductFilter products,
                                  LocalDateTime from,
                                  LocalDateTime to) {
        long orderCount = nz(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to));
        if (orderCount == 0L) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal cogs = nzBd(sumSupplierCogs(supplierId, geo, products, from, to));
        return cogs.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
    }

    private Long countDistinctDeliveredCustomers(UUID supplierId, SupplierPortalGeographyFilter geo,
                                                 SupplierPortalProductFilter products,
                                                 LocalDateTime from, LocalDateTime to) {
        return salesOrdersRepository.countDistinctDeliveredSupplierPortalCustomers(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to);
    }

    private BigDecimal sumAllProductsSettledTonnes(UUID supplierId, SupplierPortalGeographyFilter geo,
                                                   SupplierPortalProductFilter products,
                                                   LocalDateTime from, LocalDateTime to) {
        return salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to);
    }

    private BigDecimal sumSupplierCogs(UUID supplierId, SupplierPortalGeographyFilter geo,
                                       SupplierPortalProductFilter products,
                                       LocalDateTime from, LocalDateTime to) {
        return salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to);
    }

    private List<UUID> optionalUuidList(UUID id) {
        return id == null ? List.of() : List.of(id);
    }

    private CustomerAnalysisCustomerRowV2DTO mapCustomerRow(Object[] row) {
        CustomerAnalysisCustomerRowV2DTO dto = new CustomerAnalysisCustomerRowV2DTO();
        dto.setCustomerPublicId(coerceUuid(row[0]));
        dto.setShopName(row[1] != null ? row[1].toString() : null);
        dto.setRegionPublicId(coerceUuid(row[2]));
        dto.setRegionName(row[3] != null ? row[3].toString() : null);
        dto.setOrganizationLocationPublicId(coerceUuid(row[4]));
        dto.setOrganizationLocationName(row[5] != null ? row[5].toString() : "UNKNOWN");
        dto.setLandmark(row[6] != null ? row[6].toString() : null);
        dto.setDeliveredOrderCount(coerceLong(row[7]));
        dto.setTotalCogsTzs(nzBd(coerceToBigDecimal(row[8])));
        dto.setSettledTonnes(nzBd(coerceToBigDecimal(row[9])));
        dto.setLastOrderDate(coerceLocalDate(row[10]));
        dto.setProductNames(row[11] != null ? row[11].toString() : null);
        dto.setProductPurchaseQuantities(row.length > 12 && row[12] != null ? row[12].toString() : null);
        dto.setProductPurchaseDates(row.length > 13 && row[13] != null ? row[13].toString() : null);
        return dto;
    }

    private static String normalizeSearchPattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
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
        if (raw instanceof CharSequence text) {
            String value = text.toString().trim();
            if (value.isEmpty()) {
                return null;
            }
            return UUID.fromString(value);
        }
        if (raw instanceof byte[] bytes && bytes.length == 16) {
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (bytes[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (bytes[i] & 0xff);
            }
            return new UUID(msb, lsb);
        }
        throw new IllegalArgumentException("Expected UUID from JDBC bridge, got " + raw.getClass());
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

    private static LocalDate coerceLocalDate(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof LocalDate localDate) {
            return localDate;
        }
        if (raw instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (raw instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (raw instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (raw instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDate();
        }
        if (raw instanceof Instant instant) {
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

    private static void applyAverageOrderValuePercentageOfTotal(List<CustomerAnalysisDistrictRankingRowV2DTO> rows) {
        if (rows.isEmpty()) {
            return;
        }
        BigDecimal total = rows.stream()
                .map(CustomerAnalysisDistrictRankingRowV2DTO::getAverageOrderValueTzs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (CustomerAnalysisDistrictRankingRowV2DTO row : rows) {
            row.setAverageOrderValueTzsPercentageOfTotal(
                    percentShareOfTotalOrZero(row.getAverageOrderValueTzs(), total));
        }
    }

    private BigDecimal computeAverageOrderValueTzs(BigDecimal cogs, long orderCount) {
        if (orderCount == 0L) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return nzBd(cogs).divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
    }

    private static Double percentShareOfTotalOrZero(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        BigDecimal value = part != null ? part : BigDecimal.ZERO;
        return value.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .doubleValue();
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

    /**
     * Sums {@link SalesOrdersRepository#aggregateSupplierPortalSalesCalendarByDay} order column
     * (distinct {@code so.public_id} per day). Each order has one creation day, so the sum equals
     * the window-level distinct delivered-order count for the same portal filters as the trend series.
     */
    private long sumDistinctDeliveredOrdersFromDailyAggregate(UUID supplierId,
                                                              SupplierPortalGeographyFilter geo,
                                                              LocalDateTime from,
                                                              LocalDateTime to) {
        List<Object[]> raw = salesOrdersRepository.aggregateSupplierPortalSalesCalendarByDay(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(), from, to);
        long total = 0L;
        for (Object[] row : raw) {
            total += coerceLong(row[3]);
        }
        return total;
    }

    /**
     * Sums per-day distinct delivered-order counts from {@link SalesOrdersRepository#aggregateCustomerAnalysisActivityByDay}.
     * Equals the month/window distinct-order total because each order has one creation day.
     */
    private long sumDistinctDeliveredOrdersFromActivityByDay(UUID supplierId,
                                                             SupplierPortalGeographyFilter geo,
                                                             SupplierPortalProductFilter products,
                                                             LocalDateTime from,
                                                             LocalDateTime to) {
        List<Object[]> raw = salesOrdersRepository.aggregateCustomerAnalysisActivityByDay(
                supplierId, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to);
        long total = 0L;
        for (Object[] row : raw) {
            total += coerceLong(row[2]);
        }
        return total;
    }

}
