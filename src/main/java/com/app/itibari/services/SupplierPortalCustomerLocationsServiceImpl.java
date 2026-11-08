package com.app.itibari.services;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.report.CustomerLocationDetailV2DTO;
import com.app.itibari.models.report.CustomerLocationMarkerV2DTO;
import com.app.itibari.models.report.CustomerLocationPeriodMetricsV2DTO;
import com.app.itibari.models.report.CustomerLocationsHeatMapCellV2DTO;
import com.app.itibari.models.report.CustomerLocationsHeatMapMetaV2DTO;
import com.app.itibari.models.report.CustomerLocationsHeatMapV2DTO;
import com.app.itibari.models.report.CustomerLocationsMarkersMetaV2DTO;
import com.app.itibari.models.report.CustomerLocationsMarkersV2DTO;
import com.app.itibari.dataTypes.CustomerLocationMarkerCategoryV2;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.SupplierPortalCustomerLocationSnapshotRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.services.supplierportal.SupplierPortalCustomerLocationsSupport;
import com.app.itibari.services.supplierportal.SupplierPortalCustomerLocationsValidation;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.exceptions.rest.ResourceNotFoundRestApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.Principal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Supplier Portal Customer Locations V2 — additive map markers and customer detail.
 * Uses portal V2 delivered-order semantics; does not touch legacy {@code /customers/customerLocations}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierPortalCustomerLocationsServiceImpl implements SupplierPortalCustomerLocationsService {

    private static final int SQL_MARKER_LIMIT = SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1;

    private final SupplierPortalCustomerLocationSnapshotRepository snapshotRepository;
    private final SalesOrdersRepository salesOrdersRepository;
    private final PersonRepository personRepository;
    private final SupplierRepository supplierRepository;

    @Override
    public CustomerLocationsMarkersV2DTO getMarkers(UUID regionId,
                                                    UUID locationId,
                                                    UUID productId,
                                                    Principal principal) {
        Suppliers supplier = resolveSupplier(principal);

        List<Object[]> rawRows = snapshotRepository.listCustomerLocationMarkersFromSnapshot(
                supplier.getPublicId(),
                regionId,
                locationId,
                productId,
                SQL_MARKER_LIMIT);

        boolean truncated = rawRows.size() > SupplierPortalCustomerLocationsValidation.MAX_MARKERS;
        int rowsToProcess = Math.min(rawRows.size(), SupplierPortalCustomerLocationsValidation.MAX_MARKERS);
        List<CustomerLocationMarkerV2DTO> validMarkers = new ArrayList<>();
        int omittedInvalid = 0;
        // Reuse cell centers when many customers share a geohash (avoids repeat decode).
        Map<String, double[]> cellCenterByHash = new HashMap<>();

        for (int i = 0; i < rowsToProcess; i++) {
            Object[] row = rawRows.get(i);
            Optional<double[]> coords = SupplierPortalCustomerLocationsSupport.parseCoordinates(
                    row[2] != null ? row[2].toString() : null,
                    row[3] != null ? row[3].toString() : null);
            if (coords.isEmpty()) {
                omittedInvalid++;
                continue;
            }
            String geohash = SupplierPortalCustomerLocationsSupport.encodeGeohash(
                    coords.get()[0],
                    coords.get()[1],
                    SupplierPortalCustomerLocationsValidation.HEAT_MAP_GEOHASH_PRECISION);
            double[] cellCenter = cellCenterByHash.computeIfAbsent(
                    geohash, SupplierPortalCustomerLocationsSupport::decodeGeohashCenter);

            CustomerLocationMarkerV2DTO marker = new CustomerLocationMarkerV2DTO();
            marker.setCustomerPublicId(coerceUuid(row[0]));
            marker.setCustomerName(SupplierPortalCustomerLocationsSupport.normalizeCustomerName(row[1]));
            marker.setLatitude(cellCenter[0]);
            marker.setLongitude(cellCenter[1]);
            marker.setCategory(SupplierPortalCustomerLocationsSupport.resolveMarkerCategory(
                    row[4] != null ? row[4].toString() : null,
                    coerceLocalDateTime(row[5])));
            marker.setRegionPublicId(coerceUuid(row[6]));
            marker.setRegionName(row[7] != null ? row[7].toString() : null);
            marker.setOrganizationLocationPublicId(coerceUuid(row[8]));
            marker.setOrganizationLocationName(row[9] != null ? row[9].toString() : null);
            validMarkers.add(marker);
        }

        CustomerLocationsMarkersMetaV2DTO meta = new CustomerLocationsMarkersMetaV2DTO();
        meta.setReturnedCount(validMarkers.size());
        meta.setOmittedInvalidCoordinates(omittedInvalid);
        meta.setTruncated(truncated);
        meta.setMaxMarkers(SupplierPortalCustomerLocationsValidation.MAX_MARKERS);

        CustomerLocationsMarkersV2DTO response = new CustomerLocationsMarkersV2DTO();
        response.setMarkers(validMarkers);
        response.setMeta(meta);
        return response;
    }

    @Override
    public CustomerLocationsHeatMapV2DTO getHeatMap(UUID regionId,
                                                    UUID locationId,
                                                    UUID productId,
                                                    CustomerLocationMarkerCategoryV2 category,
                                                    Principal principal) {
        CustomerLocationMarkerCategoryV2 categoryFilter =
                SupplierPortalCustomerLocationsValidation.validateHeatMapCategoryFilter(category);
        Suppliers supplier = resolveSupplier(principal);

        List<Object[]> rawRows = snapshotRepository.listCustomerLocationHeatMapPointsFromSnapshot(
                supplier.getPublicId(),
                regionId,
                locationId,
                productId,
                SQL_MARKER_LIMIT);

        boolean truncated = rawRows.size() > SupplierPortalCustomerLocationsValidation.MAX_MARKERS;
        int rowsToProcess = Math.min(rawRows.size(), SupplierPortalCustomerLocationsValidation.MAX_MARKERS);

        List<CustomerLocationMarkerCategoryV2> responseCategories = categoryFilter == null
                ? SupplierPortalCustomerLocationsSupport.HEAT_MAP_CATEGORIES
                : List.of(categoryFilter);

        // category -> geohash -> customer count (exact coords never leave this method)
        Map<CustomerLocationMarkerCategoryV2, Map<String, Integer>> cellCounts =
                new EnumMap<>(CustomerLocationMarkerCategoryV2.class);
        for (CustomerLocationMarkerCategoryV2 heatMapCategory : responseCategories) {
            cellCounts.put(heatMapCategory, new LinkedHashMap<>());
        }

        for (int i = 0; i < rowsToProcess; i++) {
            Object[] row = rawRows.get(i);
            Optional<double[]> coords = SupplierPortalCustomerLocationsSupport.parseCoordinates(
                    row[1] != null ? row[1].toString() : null,
                    row[2] != null ? row[2].toString() : null);
            if (coords.isEmpty()) {
                continue;
            }
            Optional<CustomerLocationMarkerCategoryV2> resolved =
                    SupplierPortalCustomerLocationsSupport.resolveHeatMapCategory(
                            row[3] != null ? row[3].toString() : null,
                            coerceLocalDateTime(row[4]));
            if (resolved.isEmpty()) {
                continue;
            }
            CustomerLocationMarkerCategoryV2 resolvedCategory = resolved.get();
            // Filter before geohash encode — skip work for categories the caller did not request.
            if (categoryFilter != null && resolvedCategory != categoryFilter) {
                continue;
            }
            String geohash = SupplierPortalCustomerLocationsSupport.encodeGeohash(
                    coords.get()[0],
                    coords.get()[1],
                    SupplierPortalCustomerLocationsValidation.HEAT_MAP_GEOHASH_PRECISION);
            Map<String, Integer> byHash = cellCounts.get(resolvedCategory);
            byHash.put(geohash, byHash.getOrDefault(geohash, 0) + 1);
        }

        CustomerLocationsHeatMapV2DTO response = new CustomerLocationsHeatMapV2DTO();
        Map<CustomerLocationMarkerCategoryV2, List<CustomerLocationsHeatMapCellV2DTO>> points = response.getPoints();
        Map<CustomerLocationMarkerCategoryV2, Long> countByCategory =
                new EnumMap<>(CustomerLocationMarkerCategoryV2.class);
        int totalCells = 0;

        // Always expose NEW/ACTIVE/CHURNED keys for FE tab stability; unrequested stay empty.
        for (CustomerLocationMarkerCategoryV2 heatMapCategory
                : SupplierPortalCustomerLocationsSupport.HEAT_MAP_CATEGORIES) {
            List<CustomerLocationsHeatMapCellV2DTO> cells = points.get(heatMapCategory);
            long customersInCategory = 0L;
            Map<String, Integer> byHash = cellCounts.getOrDefault(heatMapCategory, Map.of());
            for (Map.Entry<String, Integer> entry : byHash.entrySet()) {
                double[] center = SupplierPortalCustomerLocationsSupport.decodeGeohashCenter(entry.getKey());
                CustomerLocationsHeatMapCellV2DTO cell = new CustomerLocationsHeatMapCellV2DTO();
                cell.setLat(center[0]);
                cell.setLng(center[1]);
                cell.setCount(entry.getValue());
                cells.add(cell);
                customersInCategory += entry.getValue();
                totalCells++;
            }
            countByCategory.put(heatMapCategory, customersInCategory);
        }

        CustomerLocationsHeatMapMetaV2DTO meta = new CustomerLocationsHeatMapMetaV2DTO();
        meta.setReturnedCount(totalCells);
        meta.setTruncated(truncated);
        meta.setMaxPoints(SupplierPortalCustomerLocationsValidation.MAX_MARKERS);
        meta.setCountByCategory(countByCategory);
        response.setMeta(meta);
        return response;
    }

    @Override
    public CustomerLocationDetailV2DTO getCustomerDetail(UUID customerId,
                                                         UUID regionId,
                                                         UUID locationId,
                                                         UUID productId,
                                                         LocalDate startDate,
                                                         LocalDate endDate,
                                                         Principal principal) {
        SupplierPortalCustomerLocationsValidation.validateDetailOptionalDateRange(startDate, endDate);
        Suppliers supplier = resolveSupplier(principal);

        List<Object[]> baseRows = salesOrdersRepository.findCustomerLocationDetailBaseV2(
                supplier.getPublicId(),
                customerId,
                regionId,
                locationId,
                productId);
        if (baseRows.isEmpty()) {
            throw new ResourceNotFoundRestApiException()
                    .developerMessage("customer not in supplier portal scope or not found: " + customerId)
                    .userMessage("Customer not found");
        }

        Object[] row = baseRows.get(0);
        CustomerLocationDetailV2DTO dto = mapDetailBase(row);

        if (startDate != null && endDate != null) {
            List<Object[]> metricsRows = salesOrdersRepository.aggregateCustomerLocationPeriodMetricsV2(
                    supplier.getPublicId(),
                    customerId,
                    regionId,
                    locationId,
                    productId,
                    dateStartInclusive(startDate),
                    dateEndInclusive(endDate));
            dto.setPeriodMetrics(mapPeriodMetrics(metricsRows.get(0), startDate, endDate));
        }

        return dto;
    }

    private CustomerLocationDetailV2DTO mapDetailBase(Object[] row) {
        CustomerLocationDetailV2DTO dto = new CustomerLocationDetailV2DTO();
        dto.setCustomerPublicId(coerceUuid(row[0]));
        dto.setCustomerName(SupplierPortalCustomerLocationsSupport.normalizeCustomerName(row[1]));
        String rawPhone = row[2] != null ? row[2].toString() : null;
        dto.setPhoneVisible(SupplierPortalCustomerLocationsSupport.isPhoneVisible(rawPhone));
        dto.setPhoneNumber(SupplierPortalCustomerLocationsSupport.resolveDetailPhoneNumber(rawPhone));

        Optional<double[]> coords = SupplierPortalCustomerLocationsSupport.parseCoordinates(
                row[3] != null ? row[3].toString() : null,
                row[4] != null ? row[4].toString() : null);
        coords.ifPresent(parsed -> {
            dto.setLatitude(parsed[0]);
            dto.setLongitude(parsed[1]);
        });

        dto.setCategory(SupplierPortalCustomerLocationsSupport.mapCategory(
                row[5] != null ? row[5].toString() : null));
        dto.setLandmark(row[6] != null ? row[6].toString() : null);
        dto.setAddress(row[7] != null ? row[7].toString() : null);
        dto.setRegionPublicId(coerceUuid(row[8]));
        dto.setRegionName(row[9] != null ? row[9].toString() : null);
        dto.setOrganizationLocationPublicId(coerceUuid(row[10]));
        dto.setOrganizationLocationName(row[11] != null ? row[11].toString() : null);
        return dto;
    }

    private CustomerLocationPeriodMetricsV2DTO mapPeriodMetrics(Object[] row,
                                                                LocalDate startDate,
                                                                LocalDate endDate) {
        CustomerLocationPeriodMetricsV2DTO metrics = new CustomerLocationPeriodMetricsV2DTO();
        metrics.setStartDate(startDate);
        metrics.setEndDate(endDate);
        metrics.setDeliveredOrderCount(coerceLong(row[0]));
        metrics.setTotalCogsTzs(nzBd(coerceToBigDecimal(row[1])));
        metrics.setSettledTonnes(nzBd(coerceToBigDecimal(row[2])));
        metrics.setLastOrderDate(coerceLocalDate(row[3]));
        return metrics;
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
        if (raw instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (raw instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (raw instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        throw new IllegalArgumentException("Expected date JDBC type, got " + raw.getClass());
    }

    private static LocalDateTime coerceLocalDateTime(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (raw instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (raw instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().atStartOfDay();
        }
        throw new IllegalArgumentException("Expected date/time JDBC type, got " + raw.getClass());
    }
}
