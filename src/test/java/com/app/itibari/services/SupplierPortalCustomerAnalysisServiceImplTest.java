package com.app.itibari.services;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.report.CustomerAnalysisActivityCalendarV2DTO;
import com.app.itibari.models.report.CustomerAnalysisCustomerRowV2DTO;
import com.app.itibari.models.report.CustomerAnalysisDistrictCogsV2DTO;
import com.app.itibari.models.report.CustomerAnalysisDistrictRankingV2DTO;
import com.app.itibari.models.report.CustomerAnalysisOrdersTrendV2DTO;
import com.app.itibari.models.report.CustomerAnalysisSummaryMetricsV2DTO;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.security.Principal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierPortalCustomerAnalysisServiceImplTest {

    private static final UUID SUPPLIER_ID = UUID.fromString("30577548-e687-4115-9d51-3a33a363aa56");
    private static final String SUPPLIER_EMAIL = "supp@email.com";

    private SalesOrdersRepository salesOrdersRepository;
    private PersonRepository personRepository;
    private SupplierRepository supplierRepository;
    private Principal principal;
    private SupplierPortalCustomerAnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        salesOrdersRepository = mock(SalesOrdersRepository.class);
        personRepository = mock(PersonRepository.class);
        supplierRepository = mock(SupplierRepository.class);
        principal = mock(Principal.class);
        service = new SupplierPortalCustomerAnalysisServiceImpl(
                salesOrdersRepository, personRepository, supplierRepository);
        stubSupplierResolution();
    }

    private void stubSupplierResolution() {
        when(principal.getName()).thenReturn(SUPPLIER_EMAIL);

        Person person = new Person();
        person.setEmail(SUPPLIER_EMAIL);
        person.setSupplierPublicId(SUPPLIER_ID.toString());
        when(personRepository.findByEmailAndDeletedIsFalse(SUPPLIER_EMAIL)).thenReturn(Optional.of(person));

        Suppliers supplier = new Suppliers();
        supplier.setPublicId(SUPPLIER_ID);
        when(supplierRepository.findByPublicIdAndDeletedIsFalse(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
    }

    @Test
    void getSummaryMetrics_mapsTrendMetricsAndComputesAov() {
        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);

        when(salesOrdersRepository.countDistinctDeliveredSupplierPortalCustomers(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(12L, 6L);
        when(salesOrdersRepository.countNewDeliveredCustomersForCustomerAnalysis(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3L, 1L);
        when(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L, 5L);
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("500000"), new BigDecimal("200000"));
        stubZeroFrequentCustomers();

        CustomerAnalysisSummaryMetricsV2DTO dto = service.getSummaryMetrics(
                List.of(), List.of(), List.of(), start, end, principal);

        assertEquals(12L, dto.getCustomers().getValue());
        assertEquals(100d, dto.getCustomers().getTrendPercentage());
        assertEquals("vs Previous Period", dto.getCustomers().getTrendLabel());
        assertEquals(3L, dto.getNewCustomers().getValue());
        assertEquals(0, new BigDecimal("50000.00").compareTo(dto.getAov().getValue()));
        assertEquals(0L, dto.getFrequentCustomers().getValue());
    }

    @Test
    void getSummaryMetrics_aovZeroWhenNoDeliveredOrders() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        when(salesOrdersRepository.countDistinctDeliveredSupplierPortalCustomers(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);
        when(salesOrdersRepository.countNewDeliveredCustomersForCustomerAnalysis(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);
        when(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);
        stubZeroFrequentCustomers();

        CustomerAnalysisSummaryMetricsV2DTO dto = service.getSummaryMetrics(
                List.of(), List.of(), List.of(), start, end, principal);

        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(dto.getAov().getValue()));
        assertEquals(0.0, dto.getFrequentCustomers().getTrendPercentage());
    }

    @Test
    void getSummaryMetrics_frequentCustomersCountAndTrendFromOrderFrequency() {
        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);

        stubSummaryMetricsBaselines();
        when(salesOrdersRepository.countFrequentDeliveredCustomersForCustomerAnalysis(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(4L, 2L);

        CustomerAnalysisSummaryMetricsV2DTO dto = service.getSummaryMetrics(
                List.of(), List.of(), List.of(), start, end, principal);

        assertEquals(4L, dto.getFrequentCustomers().getValue());
        assertEquals(100d, dto.getFrequentCustomers().getTrendPercentage());
        assertEquals("vs Previous Period", dto.getFrequentCustomers().getTrendLabel());
        assertEquals(12L, dto.getCustomers().getValue());
        assertEquals(3L, dto.getNewCustomers().getValue());
    }

    @Test
    void getSummaryMetrics_frequentCustomersZeroWhenNoRepeatOrders() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        stubSummaryMetricsBaselines();
        when(salesOrdersRepository.countFrequentDeliveredCustomersForCustomerAnalysis(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);

        CustomerAnalysisSummaryMetricsV2DTO dto = service.getSummaryMetrics(
                List.of(), List.of(), List.of(), start, end, principal);

        assertEquals(0L, dto.getFrequentCustomers().getValue());
        assertEquals(0.0, dto.getFrequentCustomers().getTrendPercentage());
    }

    @Test
    void getCustomers_mapsPagedRows() {
        UUID customerId = UUID.randomUUID();
        UUID regionId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        Object[] row = {
                customerId, "Acme Ltd",
                regionId, "Dar",
                locationId, "Kinondoni",
                "Near mall",
                4L,
                new BigDecimal("120000"),
                new BigDecimal("8.5"),
                Timestamp.valueOf(LocalDateTime.of(2026, 3, 15, 10, 0)),
                "Cement, Steel",
                "Cement: 80, Steel: 40",
                "Cement: 15/03/2026 | Steel: 10/03/2026"
        };
        when(salesOrdersRepository.pageCustomerAnalysisCustomersV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class),
                eq("%acme%"), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 20), 1));

        Page<CustomerAnalysisCustomerRowV2DTO> page = service.getCustomers(
                List.of(), List.of(), List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "Acme",
                principal,
                PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        CustomerAnalysisCustomerRowV2DTO dto = page.getContent().get(0);
        assertEquals(customerId, dto.getCustomerPublicId());
        assertEquals("Acme Ltd", dto.getShopName());
        assertEquals(4L, dto.getDeliveredOrderCount());
        assertEquals(LocalDate.of(2026, 3, 15), dto.getLastOrderDate());
        assertEquals("Cement, Steel", dto.getProductNames());
        assertEquals("Cement: 80, Steel: 40", dto.getProductPurchaseQuantities());
        assertEquals("Cement: 15/03/2026 | Steel: 10/03/2026", dto.getProductPurchaseDates());
    }

    @Test
    void getCustomers_mapsShopNameFromSqlDisplayNameIncludingPersonNameFallback() {
        // Repository COALESCE(store_name, first+last, name) already applied in SQL; mapper passes through row[1].
        UUID customerId = UUID.randomUUID();
        Object[] row = {
                customerId, "Jane Doe",
                UUID.randomUUID(), "Dar",
                UUID.randomUUID(), "Kinondoni",
                null,
                2L,
                new BigDecimal("10000"),
                new BigDecimal("1.0"),
                Timestamp.valueOf(LocalDateTime.of(2026, 3, 12, 8, 0)),
                "Sugar",
                "Sugar: 5",
                "Sugar: 12/03/2026"
        };
        when(salesOrdersRepository.pageCustomerAnalysisCustomersV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(null), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 20), 1));

        Page<CustomerAnalysisCustomerRowV2DTO> page = service.getCustomers(
                List.of(), List.of(), List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null,
                principal,
                PageRequest.of(0, 20));

        assertEquals("Jane Doe", page.getContent().get(0).getShopName());
    }

    @Test
    void getCustomers_mapsNullProductNamesWhenCustomerHasNoProducts() {
        UUID customerId = UUID.randomUUID();
        Object[] row = {
                customerId, "Solo Buyer",
                UUID.randomUUID(), "Dar",
                UUID.randomUUID(), "Kinondoni",
                null,
                1L,
                new BigDecimal("50000"),
                new BigDecimal("2.0"),
                Timestamp.valueOf(LocalDateTime.of(2026, 3, 10, 9, 0)),
                null,
                null,
                null
        };
        when(salesOrdersRepository.pageCustomerAnalysisCustomersV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(null), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 20), 1));

        Page<CustomerAnalysisCustomerRowV2DTO> page = service.getCustomers(
                List.of(), List.of(), List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null,
                principal,
                PageRequest.of(0, 20));

        assertEquals(1L, page.getContent().get(0).getDeliveredOrderCount());
        assertNull(page.getContent().get(0).getProductNames());
    }

    @Test
    void getCustomers_mapsUuidColumnsFromStringJdbcValues() {
        UUID customerId = UUID.randomUUID();
        UUID regionId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        Object[] row = {
                customerId.toString(), "Acme Ltd",
                regionId.toString(), "Dar",
                locationId.toString(), "Kinondoni",
                null,
                1L,
                new BigDecimal("50000"),
                new BigDecimal("2.0"),
                Date.valueOf(LocalDate.of(2026, 3, 10)),
                "Cement",
                "Cement: 10",
                "Cement: 10/03/2026"
        };
        when(salesOrdersRepository.pageCustomerAnalysisCustomersV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(null), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 20), 1));

        Page<CustomerAnalysisCustomerRowV2DTO> page = service.getCustomers(
                List.of(), List.of(), List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null,
                principal,
                PageRequest.of(0, 20));

        CustomerAnalysisCustomerRowV2DTO dto = page.getContent().get(0);
        assertEquals(customerId, dto.getCustomerPublicId());
        assertEquals(regionId, dto.getRegionPublicId());
        assertEquals(locationId, dto.getOrganizationLocationPublicId());
    }

    @Test
    void getCustomers_mapsLastOrderDateFromPostgresJdbcTypes() {
        UUID customerId = UUID.randomUUID();
        Object[] sqlDateRow = customerRow(customerId, Date.valueOf(LocalDate.of(2026, 3, 15)));
        Object[] offsetRow = customerRow(customerId, OffsetDateTime.of(2026, 3, 16, 14, 30, 0, 0, ZoneOffset.UTC));
        Object[] instantRow = customerRow(customerId, Instant.parse("2026-03-17T08:00:00Z"));

        when(salesOrdersRepository.pageCustomerAnalysisCustomersV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(null), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(sqlDateRow, offsetRow, instantRow), PageRequest.of(0, 20), 3));

        Page<CustomerAnalysisCustomerRowV2DTO> page = service.getCustomers(
                List.of(), List.of(), List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null,
                principal,
                PageRequest.of(0, 20));

        assertEquals(LocalDate.of(2026, 3, 15), page.getContent().get(0).getLastOrderDate());
        assertEquals(LocalDate.of(2026, 3, 16), page.getContent().get(1).getLastOrderDate());
        assertEquals(
                Instant.parse("2026-03-17T08:00:00Z").atZone(ZoneId.of("Africa/Nairobi")).toLocalDate(),
                page.getContent().get(2).getLastOrderDate());
    }

    private static Object[] customerRow(UUID customerId, Object lastOrderDate) {
        return new Object[]{
                customerId, "Acme Ltd",
                UUID.randomUUID(), "Dar",
                UUID.randomUUID(), "Kinondoni",
                "Near mall",
                2L,
                new BigDecimal("80000"),
                new BigDecimal("4.0"),
                lastOrderDate,
                "Cement",
                "Cement: 20",
                "Cement: 15/03/2026"
        };
    }

    @Test
    void getCustomersForExport_usesUnpagedListQueryWithoutCountScan() {
        UUID customerId = UUID.randomUUID();
        Object[] row = customerRow(customerId, Date.valueOf(LocalDate.of(2026, 3, 15)));

        when(salesOrdersRepository.listCustomerAnalysisCustomersForExportV2(
                eq(SUPPLIER_ID), any(), any(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(null)))
                .thenReturn(Collections.singletonList(row));

        Page<CustomerAnalysisCustomerRowV2DTO> page = service.getCustomersForExport(
                null, null,
                List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null,
                principal,
                PageRequest.of(0, 2000));

        assertEquals(1, page.getContent().size());
        verify(salesOrdersRepository).listCustomerAnalysisCustomersForExportV2(
                eq(SUPPLIER_ID), any(), any(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(null));
        verify(salesOrdersRepository, never()).pageCustomerAnalysisCustomersV2(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void getCustomersForExport_mapsOneRowPerOrderFromExportQuery() {
        UUID customerId = UUID.randomUUID();
        Object[] order1 = customerRow(customerId, Date.valueOf(LocalDate.of(2026, 3, 10)));
        order1[1] = "Acme Shop";
        order1[7] = 1L;
        order1[11] = "Sugar";
        order1[12] = "Sugar: 10";
        order1[13] = "Sugar: 10/03/2026";

        Object[] order2 = customerRow(customerId, Date.valueOf(LocalDate.of(2026, 3, 20)));
        order2[1] = "Acme Shop";
        order2[7] = 1L;
        order2[11] = "Flour";
        order2[12] = "Flour: 5";
        order2[13] = "Flour: 20/03/2026";

        when(salesOrdersRepository.listCustomerAnalysisCustomersForExportV2(
                eq(SUPPLIER_ID), any(), any(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(null)))
                .thenReturn(List.of(order1, order2));

        Page<CustomerAnalysisCustomerRowV2DTO> page = service.getCustomersForExport(
                null, null,
                List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null,
                principal,
                PageRequest.of(0, 2000));

        assertEquals(2, page.getContent().size());
        assertEquals(1L, page.getContent().get(0).getDeliveredOrderCount());
        assertEquals(1L, page.getContent().get(1).getDeliveredOrderCount());
        assertEquals(LocalDate.of(2026, 3, 10), page.getContent().get(0).getLastOrderDate());
        assertEquals(LocalDate.of(2026, 3, 20), page.getContent().get(1).getLastOrderDate());
        assertEquals("Sugar", page.getContent().get(0).getProductNames());
        assertEquals("Flour", page.getContent().get(1).getProductNames());
    }

    @Test
    void getCustomers_passesRegionAndLocationFiltersToRepository() {
        UUID regionId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(salesOrdersRepository.pageCustomerAnalysisCustomersV2(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class), any(), any()))
                .thenReturn(Page.empty());

        service.getCustomers(
                regionId,
                locationId,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null,
                principal,
                PageRequest.of(0, 20));

        org.mockito.ArgumentCaptor<List> regionCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.ArgumentCaptor<List> locationCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(salesOrdersRepository).pageCustomerAnalysisCustomersV2(
                eq(SUPPLIER_ID),
                regionCaptor.capture(),
                eq(false),
                locationCaptor.capture(),
                eq(false), any(), anyBoolean(),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(null),
                any());
        assertEquals(List.of(regionId), regionCaptor.getValue());
        assertEquals(List.of(locationId), locationCaptor.getValue());
    }

    @Test
    void getCustomers_passesProductFiltersToRepository_samePatternAsGeography() {
        UUID regionId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        when(salesOrdersRepository.pageCustomerAnalysisCustomersV2(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class), any(), any()))
                .thenReturn(Page.empty());

        service.getCustomers(
                List.of(regionId),
                List.of(locationId),
                List.of(productA, productB),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null,
                principal,
                PageRequest.of(0, 20));

        org.mockito.ArgumentCaptor<List> productCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(salesOrdersRepository).pageCustomerAnalysisCustomersV2(
                eq(SUPPLIER_ID),
                eq(List.of(regionId)),
                eq(false),
                eq(List.of(locationId)),
                eq(false),
                productCaptor.capture(),
                eq(false),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(null),
                any());
        assertEquals(List.of(productA, productB), productCaptor.getValue());
    }

    @Test
    void getSummaryMetrics_emptyProductIdsDisablesProductFilter() {
        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);
        stubSummaryMetricsBaselines();
        stubZeroFrequentCustomers();

        service.getSummaryMetrics(List.of(), List.of(), List.of(), start, end, principal);

        verify(salesOrdersRepository, atLeastOnce()).countDistinctDeliveredSupplierPortalCustomers(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(), eq(true),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getCustomers_rejectsClientSort() {
        assertThrows(BadRequestRestApiException.class, () -> service.getCustomers(
                List.of(),
                List.of(),
                List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null,
                principal,
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("totalCogsTzs")))));

        verify(salesOrdersRepository, never()).pageCustomerAnalysisCustomersV2(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void getDistrictRanking_noFilters_usesRegionRankingAndMapsBucketFields() {
        UUID regionA = UUID.randomUUID();
        UUID regionB = UUID.randomUUID();
        Object[] first = {regionA, "Dar es Salaam", 8L, 12L, new BigDecimal("900000")};
        Object[] second = {regionB, "Arusha", 5L, 7L, new BigDecimal("400000")};

        when(salesOrdersRepository.listCustomerAnalysisRegionRankingV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(first, second));

        CustomerAnalysisDistrictRankingV2DTO dto = service.getDistrictRanking(
                List.of(), List.of(), List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                1,
                principal);

        assertEquals(1, dto.getRows().size());
        assertEquals(1, dto.getRows().get(0).getRank());
        assertEquals("Dar es Salaam", dto.getRows().get(0).getOrganizationLocationName());
        assertEquals(regionA, dto.getRows().get(0).getOrganizationLocationPublicId());
        assertEquals(8L, dto.getRows().get(0).getDistinctCustomerCount());
        assertEquals(12L, dto.getRows().get(0).getDeliveredOrderCount());
        assertEquals(0, new BigDecimal("75000.00").compareTo(dto.getRows().get(0).getAverageOrderValueTzs()));
        assertEquals(100.0d, dto.getRows().get(0).getAverageOrderValueTzsPercentageOfTotal());
        verify(salesOrdersRepository, never()).listCustomerAnalysisDistrictRankingV2(
                any(), any(), any(), any(), any());
    }

    @Test
    void getDistrictRanking_setsAverageOrderValuePercentageOfTotalAcrossReturnedRows() {
        UUID regionA = UUID.randomUUID();
        UUID regionB = UUID.randomUUID();
        Object[] first = {regionA, "Dar es Salaam", 8L, 12L, new BigDecimal("900000")};
        Object[] second = {regionB, "Arusha", 5L, 7L, new BigDecimal("400000")};

        when(salesOrdersRepository.listCustomerAnalysisRegionRankingV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(first, second));
        when(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(19L);
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1300000"));

        CustomerAnalysisDistrictRankingV2DTO dto = service.getDistrictRanking(
                List.of(), List.of(), List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                10,
                principal);

        assertEquals(2, dto.getRows().size());
        assertEquals(0, new BigDecimal("75000.00").compareTo(dto.getRows().get(0).getAverageOrderValueTzs()));
        assertEquals(0, new BigDecimal("57142.86").compareTo(dto.getRows().get(1).getAverageOrderValueTzs()));
        assertEquals(56.76d, dto.getRows().get(0).getAverageOrderValueTzsPercentageOfTotal());
        assertEquals(43.24d, dto.getRows().get(1).getAverageOrderValueTzsPercentageOfTotal());
        assertEquals(0, new BigDecimal("68421.05").compareTo(dto.getTotalAverageOrderValueTzs()));
    }

    @Test
    void getDistrictRanking_zeroTotalRevenue_setsAverageOrderValueAndPercentageToZero() {
        UUID regionA = UUID.randomUUID();
        Object[] first = {regionA, "Dar es Salaam", 0L, 0L, BigDecimal.ZERO};

        when(salesOrdersRepository.listCustomerAnalysisRegionRankingV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(first));

        CustomerAnalysisDistrictRankingV2DTO dto = service.getDistrictRanking(
                List.of(), List.of(), List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                10,
                principal);

        assertEquals(1, dto.getRows().size());
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(dto.getRows().get(0).getAverageOrderValueTzs()));
        assertEquals(0.0d, dto.getRows().get(0).getAverageOrderValueTzsPercentageOfTotal());
    }

    @Test
    void getDistrictRanking_regionFilter_usesWardRankingUnchanged() {
        UUID regionId = UUID.randomUUID();
        UUID wardA = UUID.randomUUID();
        Object[] first = {wardA, "Kinondoni", 8L, 12L, new BigDecimal("900000")};

        when(salesOrdersRepository.listCustomerAnalysisDistrictRankingV2(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), any(), eq(true), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(first));

        CustomerAnalysisDistrictRankingV2DTO dto = service.getDistrictRanking(
                regionId, null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                10,
                principal);

        assertEquals(1, dto.getRows().size());
        assertEquals("Kinondoni", dto.getRows().get(0).getOrganizationLocationName());
        verify(salesOrdersRepository, never()).listCustomerAnalysisRegionRankingV2(
                any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getDistrictRanking_locationFilter_usesWardRankingUnchanged() {
        UUID locationId = UUID.randomUUID();
        Object[] first = {locationId, "Kinondoni", 3L, 4L, new BigDecimal("100000")};

        when(salesOrdersRepository.listCustomerAnalysisDistrictRankingV2(
                eq(SUPPLIER_ID), any(), eq(true), eq(List.of(locationId)), eq(false), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(first));

        CustomerAnalysisDistrictRankingV2DTO dto = service.getDistrictRanking(
                null, locationId,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                10,
                principal);

        assertEquals(1, dto.getRows().size());
        verify(salesOrdersRepository, never()).listCustomerAnalysisRegionRankingV2(
                any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getDistrictRanking_rejectsInvalidLimit() {
        assertThrows(BadRequestRestApiException.class, () -> service.getDistrictRanking(
                List.of(),
                List.of(),
                List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                0,
                principal));

        verify(salesOrdersRepository, never()).listCustomerAnalysisDistrictRankingV2(
                any(), any(), any(), any(), any());
        verify(salesOrdersRepository, never()).listCustomerAnalysisRegionRankingV2(
                any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getDistrictCogs_noFilters_usesRegionCogsAndLimitsRows() {
        UUID regionA = UUID.randomUUID();
        Object[] first = {regionA, "Dar es Salaam", new BigDecimal("900000")};
        Object[] second = {UUID.randomUUID(), "Arusha", new BigDecimal("400000")};

        when(salesOrdersRepository.listCustomerAnalysisRegionCogsV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Arrays.asList(first, second));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1300000"));

        CustomerAnalysisDistrictCogsV2DTO dto = service.getDistrictCogs(
                List.of(), List.of(), List.of(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                1,
                principal);

        assertEquals(1, dto.getRows().size());
        assertEquals("Dar es Salaam", dto.getRows().get(0).getOrganizationLocationName());
        assertEquals(0, new BigDecimal("1300000").compareTo(dto.getTotalCogsTzs()));
        verify(salesOrdersRepository).listCustomerAnalysisRegionCogsV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, never()).listSupplierPortalAllProductsWardCogsByTotalCogs(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getDistrictCogs_regionFilter_usesWardCogsUnchanged() {
        UUID regionId = UUID.randomUUID();
        UUID wardA = UUID.randomUUID();
        Object[] first = {wardA, "Kinondoni", new BigDecimal("900000")};

        when(salesOrdersRepository.listSupplierPortalAllProductsWardCogsByTotalCogs(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), any(), eq(true), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.singletonList(first));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), any(), eq(true), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("900000"));

        CustomerAnalysisDistrictCogsV2DTO dto = service.getDistrictCogs(
                regionId, null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                10,
                principal);

        assertEquals("Kinondoni", dto.getRows().get(0).getOrganizationLocationName());
        verify(salesOrdersRepository, never()).listCustomerAnalysisRegionCogsV2(any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getOrdersTrend_zeroFillsDailySeries_headlineEqualsSumOfDailyDistinctOrders() {
        Object[] dayTwo = {
                Date.valueOf(LocalDate.of(2026, 3, 2)),
                2L,
                4L
        };

        when(salesOrdersRepository.aggregateCustomerAnalysisActivityByDay(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.singletonList(dayTwo), java.util.Collections.emptyList());

        CustomerAnalysisOrdersTrendV2DTO dto = service.getOrdersTrend(
                List.of(), List.of(), List.of(), 3, 2026, principal);

        assertEquals(31, dto.getSeries().size());
        assertEquals(LocalDate.of(2026, 3, 1), dto.getSeries().get(0).getDate());
        assertEquals(LocalDate.of(2026, 3, 31), dto.getSeries().get(30).getDate());
        assertEquals(0L, dto.getSeries().get(0).getDeliveredOrderCount());
        assertEquals(4L, dto.getSeries().get(1).getDeliveredOrderCount());
        assertEquals(4L, dto.getHeadline().getValue());
        assertEquals(
                dto.getSeries().stream().mapToLong(p -> p.getDeliveredOrderCount()).sum(),
                dto.getHeadline().getValue());
        assertNull(dto.getHeadline().getTrendPercentage());
        assertEquals("vs Previous Period", dto.getHeadline().getTrendLabel());
        verify(salesOrdersRepository, never()).countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getOrdersTrend_resolvesMonthYearToFullCalendarWindow() {
        when(salesOrdersRepository.aggregateCustomerAnalysisActivityByDay(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.emptyList());

        service.getOrdersTrend(List.of(), List.of(), List.of(), 2, 2026, principal);

        org.mockito.ArgumentCaptor<LocalDateTime> fromCaptor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        org.mockito.ArgumentCaptor<LocalDateTime> toCaptor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        // aggregateCustomerAnalysisActivityByDay is called twice:
        //   [0] current period (Feb 2026) — from buildOrdersTrend
        //   [1] previous period (Jan 2026) — from sumDistinctDeliveredOrdersFromActivityByDay
        verify(salesOrdersRepository, org.mockito.Mockito.times(2)).aggregateCustomerAnalysisActivityByDay(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), fromCaptor.capture(), toCaptor.capture());
        assertEquals(LocalDateTime.of(2026, 2, 1, 0, 0), fromCaptor.getAllValues().get(0));
        // dateEndInclusive returns 23:59:59.999999999 (end-of-second with nanos); truncate to seconds for comparison.
        assertEquals(LocalDateTime.of(2026, 2, 28, 23, 59, 59),
                toCaptor.getAllValues().get(0).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void getOrdersTrend_rejectsMonthWithoutYear() {
        assertThrows(BadRequestRestApiException.class, () -> service.getOrdersTrend(
                List.of(), List.of(), List.of(), 3, null, principal));

        verify(salesOrdersRepository, never()).aggregateCustomerAnalysisActivityByDay(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getOrdersTrend_yearOnly_usesTwelveMonthlyBucketsAndLastYearHeadline() {
        when(salesOrdersRepository.aggregateSupplierPortalDeliveredOrdersByCreationMonth(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Arrays.asList(
                                new Object[]{2025, 1, 10L},
                                new Object[]{2025, 2, 5L}),
                        java.util.Collections.singletonList(new Object[]{2024, 12, 12L}));

        CustomerAnalysisOrdersTrendV2DTO dto = service.getOrdersTrend(
                List.of(), List.of(), List.of(), null, 2025, principal);

        assertEquals(12, dto.getSeries().size());
        assertEquals(LocalDate.of(2025, 1, 1), dto.getSeries().get(0).getDate());
        assertEquals(LocalDate.of(2025, 12, 1), dto.getSeries().get(11).getDate());
        assertEquals(10L, dto.getSeries().get(0).getDeliveredOrderCount());
        assertEquals(0L, dto.getSeries().get(2).getDeliveredOrderCount());
        assertEquals(15L, dto.getHeadline().getValue());
        assertEquals(
                dto.getSeries().stream().mapToLong(p -> p.getDeliveredOrderCount()).sum(),
                dto.getHeadline().getValue());
        assertEquals("vs Last Year", dto.getHeadline().getTrendLabel());

        verify(salesOrdersRepository, never()).aggregateCustomerAnalysisActivityByDay(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getActivityCalendar_zeroFillsAllDaysAndAssignsIntensity() {
        Object[] activeDay = {
                Date.valueOf(LocalDate.of(2026, 2, 10)),
                8L,
                12L
        };
        when(salesOrdersRepository.aggregateCustomerAnalysisActivityByDay(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(activeDay), Collections.emptyList());
        stubActivityCalendarSettledTonnes(new BigDecimal("6.5"), BigDecimal.ZERO);

        CustomerAnalysisActivityCalendarV2DTO dto = service.getActivityCalendar(
                List.of(), List.of(), List.of(), 2, 2026, principal);

        assertEquals(28, dto.getDays().size());
        assertEquals("February 2026", dto.getMonthLabel());
        assertEquals(0L, dto.getDays().get(0).getDistinctCustomerCount());
        assertEquals(8L, dto.getDays().get(9).getDistinctCustomerCount());
        assertEquals(12L, dto.getDays().get(9).getDeliveredOrderCount());
        assertEquals(1.0d, dto.getDays().get(9).getIntensity(), 0.0001d);
        assertEquals("PEAK", dto.getDays().get(9).getIntensityLevel());
        assertEquals("NONE", dto.getDays().get(0).getIntensityLevel());

        assertEquals(LocalDate.of(2026, 2, 10), dto.getSummary().getPeakDay().getDate());
        assertEquals(8L, dto.getSummary().getPeakDay().getDistinctCustomerCount());
        assertEquals(12L, dto.getSummary().getTotalOrders().getValue());
        assertEquals(0.0d, dto.getSummary().getTotalOrders().getTrendPercentage());
        assertEquals(0, new BigDecimal("6.5").compareTo(dto.getSummary().getUnitsSold().getValue()));
        assertEquals(0.0d, dto.getSummary().getUnitsSold().getTrendPercentage());
        assertEquals("vs Previous Period", dto.getSummary().getTotalOrders().getTrendLabel());
    }

    @Test
    void getActivityCalendar_peakDayTieResolvesToEarliestDate() {
        Object[] laterPeak = {Date.valueOf(LocalDate.of(2026, 2, 10)), 8L, 5L};
        Object[] earlierPeak = {Date.valueOf(LocalDate.of(2026, 2, 5)), 8L, 3L};
        when(salesOrdersRepository.aggregateCustomerAnalysisActivityByDay(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(laterPeak, earlierPeak), Collections.emptyList());
        stubActivityCalendarSettledTonnes(BigDecimal.ZERO, BigDecimal.ZERO);

        CustomerAnalysisActivityCalendarV2DTO dto = service.getActivityCalendar(
                List.of(), List.of(), List.of(), 2, 2026, principal);

        assertEquals(LocalDate.of(2026, 2, 5), dto.getSummary().getPeakDay().getDate());
        assertEquals(3L, dto.getSummary().getPeakDay().getDeliveredOrderCount());
    }

    @Test
    void getActivityCalendar_unitsSoldUsesSettledTonnesAggregateNotDayCells() {
        Object[] dayOne = {Date.valueOf(LocalDate.of(2026, 3, 1)), 10L, 4L};
        Object[] dayTwo = {Date.valueOf(LocalDate.of(2026, 3, 2)), 10L, 6L};
        when(salesOrdersRepository.aggregateCustomerAnalysisActivityByDay(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(dayOne, dayTwo), Collections.emptyList());
        stubActivityCalendarSettledTonnes(new BigDecimal("10.0"), new BigDecimal("5.0"));

        CustomerAnalysisActivityCalendarV2DTO dto = service.getActivityCalendar(
                List.of(), List.of(), List.of(), 3, 2026, principal);

        assertEquals(20L, dto.getDays().stream().mapToLong(d -> d.getDistinctCustomerCount()).sum());
        assertEquals(0, new BigDecimal("10.0").compareTo(dto.getSummary().getUnitsSold().getValue()));
        assertEquals(10L, dto.getSummary().getTotalOrders().getValue());
        assertEquals(100d, dto.getSummary().getUnitsSold().getTrendPercentage());
    }

    @Test
    void getActivityCalendar_summaryTrendsRoundedAndZeroBaseline() {
        Object[] activeDay = {Date.valueOf(LocalDate.of(2026, 4, 15)), 3L, 7L};
        when(salesOrdersRepository.aggregateCustomerAnalysisActivityByDay(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(activeDay), Collections.emptyList());
        stubActivityCalendarSettledTonnes(new BigDecimal("3.0"), BigDecimal.ZERO);

        CustomerAnalysisActivityCalendarV2DTO dto = service.getActivityCalendar(
                List.of(), List.of(), List.of(), 4, 2026, principal);

        assertEquals(0.0d, dto.getSummary().getUnitsSold().getTrendPercentage());
        assertEquals(0.0d, dto.getSummary().getTotalOrders().getTrendPercentage());
    }

    @Test
    void getActivityCalendar_nonZeroBaselineTrendTwoDecimalPrecision() {
        Object[] currentDay = {Date.valueOf(LocalDate.of(2026, 5, 10)), 5L, 10L};
        Object[] previousDay = {Date.valueOf(LocalDate.of(2026, 4, 10)), 4L, 6L};
        when(salesOrdersRepository.aggregateCustomerAnalysisActivityByDay(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(currentDay), Collections.singletonList(previousDay));
        stubActivityCalendarSettledTonnes(new BigDecimal("5"), new BigDecimal("4"));

        CustomerAnalysisActivityCalendarV2DTO dto = service.getActivityCalendar(
                List.of(), List.of(), List.of(), 5, 2026, principal);

        // (10 - 6) * 100 / 6 = 66.666... → rounds to 66.67 at scale 2
        assertEquals(66.67d, dto.getSummary().getTotalOrders().getTrendPercentage());
        assertEquals(25d, dto.getSummary().getUnitsSold().getTrendPercentage());
    }

    @Test
    void getActivityCalendar_rejectsInvalidMonth() {
        assertThrows(BadRequestRestApiException.class, () -> service.getActivityCalendar(
                List.of(), List.of(), List.of(), 13, 2026, principal));

        verify(salesOrdersRepository, never()).aggregateCustomerAnalysisActivityByDay(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    private void stubActivityCalendarSettledTonnes(BigDecimal current, BigDecimal previous) {
        when(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(current, previous);
    }

    private void stubZeroFrequentCustomers() {
        when(salesOrdersRepository.countFrequentDeliveredCustomersForCustomerAnalysis(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);
    }

    private void stubSummaryMetricsBaselines() {
        when(salesOrdersRepository.countDistinctDeliveredSupplierPortalCustomers(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(12L, 0L);
        when(salesOrdersRepository.countNewDeliveredCustomersForCustomerAnalysis(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3L, 0L);
        when(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L, 0L);
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
