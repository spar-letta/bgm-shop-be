package com.app.itibari.repository;

import com.app.itibari.services.supplierportal.SupplierPortalGeographyFilter;
import com.app.itibari.services.supplierportal.SupplierPortalProductFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.app.itibari.models.report.SupplierPortalDashboardSummaryMetricsRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Parity: consolidated aggregate must match the four headline summary-metrics queries on H2 seed data.
 * Active customers use canonical DISTINCT delivered-customer semantics shared across portal V2 summary endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "TRA_BASE_URL=localhost",
        "TRA_RECEIPT_ENDPOINT=/receipt",
        "TRA_BRANCH=test",
        "TRA_DEPARTMENT=test",
        "TRA_DEVICE_NUMBER=1",
        "TRA_CUSTOMER_ID_TYPE=1",
        "TRA_PAY_TYPE=1",
        "TRA_FCODE=test",
        "TRA_FCODE_TOKEN=test",
        "TRA_INVOICE_PREFIX=T"
})
class SalesOrdersRepositorySupplierPortalDashboardSummaryParityTest {

    private static final UUID SUPPLIER_ID = UUID.fromString("30577548-e687-4115-9d51-3a33a363aa56");

    @Autowired
    private SalesOrdersRepository salesOrdersRepository;

    @Test
    void aggregateSupplierPortalDashboardSummaryMetricsV2_matchesLegacyFourQueries() {
        LocalDateTime from = LocalDate.of(2026, 1, 1).atStartOfDay();
        LocalDateTime to = LocalDate.of(2026, 12, 31).atTime(LocalTime.MAX);

        Object[] rawRow = salesOrdersRepository.aggregateSupplierPortalDashboardSummaryMetricsV2(
                SUPPLIER_ID, null, null, from, to);
        assertNotNull(rawRow);
        SupplierPortalDashboardSummaryMetricsRow consolidated =
                SupplierPortalDashboardSummaryMetricsRow.fromAggregateRow(rawRow);

        SupplierPortalGeographyFilter geo = SupplierPortalGeographyFilter.of((UUID) null, null);
        SupplierPortalProductFilter products = SupplierPortalProductFilter.none();

        long legacyActive = nz(salesOrdersRepository.countDistinctDeliveredSupplierPortalCustomers(
                SUPPLIER_ID, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to));
        long legacyOrders = nz(salesOrdersRepository.countDeliveredOrdersBySupplierRegionAndLocation(
                SUPPLIER_ID, null, null, from, to));
        BigDecimal legacyTonnes = nzBd(salesOrdersRepository.sumWeightDeliveredTonnesBySupplierRegionAndLocation(
                SUPPLIER_ID, null, null, from, to));
        BigDecimal legacyCogs = nzBd(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                SUPPLIER_ID, geo.regionIdsParam(), geo.regionFilterDisabledParam(),
                geo.locationIdsParam(), geo.locationFilterDisabledParam(),
                products.productIdsParam(), products.productFilterDisabledParam(), from, to));

        assertEquals(legacyActive, consolidated.activeCustomers(), "activeCustomers");
        assertEquals(legacyOrders, consolidated.totalOrders(), "totalOrders");
        assertEquals(0, legacyTonnes.compareTo(consolidated.totalQuantitySoldTonnes()), "totalQuantitySoldTonnes");
        assertEquals(0, legacyCogs.compareTo(consolidated.totalCogsTzs()), "totalCogsTzs");
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }

    private static BigDecimal nzBd(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
