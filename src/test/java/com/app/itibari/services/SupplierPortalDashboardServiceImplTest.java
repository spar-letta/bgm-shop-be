package com.app.itibari.services;

import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.report.DistrictPerformanceV2PageDTO;
import com.app.itibari.models.report.DistrictPerformanceV2RowDTO;
import com.app.itibari.models.report.DistrictPerformanceV2TrendsDTO;
import com.app.itibari.models.report.ProductDistributionV2RowDTO;
import com.app.itibari.models.report.ProductDistributionV2TrendsDTO;
import com.app.itibari.models.report.SalesCalendarDayV2DTO;
import com.app.itibari.models.report.SalesCalendarV2DTO;
import com.app.itibari.models.report.SalesTrendV2DTO;
import com.app.itibari.models.report.SummaryCardsV2DTO;
import com.app.itibari.models.response.FileResponseDTO;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.services.helper.ProductImageResolver;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierPortalDashboardServiceImplTest {

    private static final UUID SUPPLIER_ID = UUID.fromString("30577548-e687-4115-9d51-3a33a363aa56");
    private static final String SUPPLIER_EMAIL = "supp@email.com";

    private SalesOrdersRepository salesOrdersRepository;
    private PersonRepository personRepository;
    private SupplierRepository supplierRepository;
    private FileService fileService;
    private ProductImageResolver productImageResolver;
    private Principal principal;
    private SupplierPortalDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        salesOrdersRepository = mock(SalesOrdersRepository.class);
        personRepository = mock(PersonRepository.class);
        supplierRepository = mock(SupplierRepository.class);
        fileService = mock(FileService.class);
        productImageResolver = new ProductImageResolver(fileService);
        principal = mock(Principal.class);
        service = new SupplierPortalDashboardServiceImpl(
                salesOrdersRepository, personRepository, supplierRepository, productImageResolver);
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
    void getProductDistributionV2_mapsRowsAssignsRanksAndUsesV2Repository() throws IOException {
        UUID productId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(1, 10);
        Object[] row = productDistributionRow(
                productId,
                "Sugar 50kg",
                "SKU-1",
                "Bag",
                120L,
                new BigDecimal("500000"),
                new BigDecimal("6.5"));
        when(salesOrdersRepository.pageSupplierPortalProductDistributionV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.singletonList(row), pageable, 42));
        when(salesOrdersRepository.listSupplierPortalProductDistributionV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        when(fileService.getImages(eq(productId), eq(ReferenceType.PRODUCT)))
                .thenReturn(new FileResponseDTO("https://cdn.example/img.png", "image/png", null, null, true));

        Page<ProductDistributionV2RowDTO> page = service.getProductDistributionV2(
                List.of(), List.of(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), principal, pageable);

        assertEquals(1, page.getContent().size());
        ProductDistributionV2RowDTO dto = page.getContent().get(0);
        assertEquals(11, dto.getRank());
        assertEquals(productId, dto.getProductPublicId());
        assertEquals("Sugar 50kg", dto.getProductName());
        assertEquals("SKU-1", dto.getSku());
        assertEquals("Bag", dto.getUom());
        assertEquals(120L, dto.getUnitsSold());
        assertEquals(0, new BigDecimal("500000").compareTo(dto.getSupplierLineRevenueTzs()));
        assertEquals(0, new BigDecimal("6.5").compareTo(dto.getSettledTonnes()));
        assertEquals("https://cdn.example/img.png", dto.getProductImageUrl());
        assertEquals(42, page.getTotalElements());
        assertNotNull(dto.getPeriodTrends());
        assertEquals("vs Previous Period", dto.getPeriodTrends().getTrendLabel());
        assertEquals(0.0d, dto.getPeriodTrends().getUnitsSoldTrendPercentage(), 0.0001d);

        verify(salesOrdersRepository).pageSupplierPortalProductDistributionV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable));
        verify(salesOrdersRepository).listSupplierPortalProductDistributionV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, never()).countActiveCustomers(any(), any(), any(), any());
    }

    @Test
    void getProductDistributionV2_imageEnrichment_toleratesUrlEmptyAndLookupFailures() throws IOException {
        UUID withUrl = UUID.randomUUID();
        UUID emptyUrl = UUID.randomUUID();
        UUID failing = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        List<Object[]> rows = Arrays.asList(
                productDistributionRow(withUrl, "A", null, null, 1L, BigDecimal.ONE, BigDecimal.ONE),
                productDistributionRow(emptyUrl, "B", null, null, 2L, BigDecimal.ONE, BigDecimal.ONE),
                productDistributionRow(failing, "C", null, null, 3L, BigDecimal.ONE, BigDecimal.ONE));
        when(salesOrdersRepository.pageSupplierPortalProductDistributionV2(
                any(), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(rows, pageable, 3));
        when(salesOrdersRepository.listSupplierPortalProductDistributionV2(
                any(), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        when(fileService.getImages(withUrl, ReferenceType.PRODUCT))
                .thenReturn(new FileResponseDTO("https://cdn.example/a.png", "image/png", null, null, true));
        when(fileService.getImages(emptyUrl, ReferenceType.PRODUCT))
                .thenReturn(new FileResponseDTO("", "image/png", null, null, true));
        when(fileService.getImages(failing, ReferenceType.PRODUCT))
                .thenThrow(new RuntimeException("storage client failure"));

        Page<ProductDistributionV2RowDTO> page = service.getProductDistributionV2(
                List.of(), List.of(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), principal, pageable);

        assertEquals(3, page.getContent().size());
        assertEquals("https://cdn.example/a.png", page.getContent().get(0).getProductImageUrl());
        assertNull(page.getContent().get(1).getProductImageUrl());
        assertNull(page.getContent().get(2).getProductImageUrl());
        assertEquals(1, page.getContent().get(0).getRank());
        assertEquals(3, page.getContent().get(2).getRank());
    }

    @Test
    void getProductDistributionV2_mergesPreviousWindowByProductId() {
        UUID productId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);
        Object[] currentRow = productDistributionRow(productId, "Sugar", "SKU", "Bag", 120L, BigDecimal.TEN, BigDecimal.ONE);
        Object[] previousRow = productDistributionRow(productId, "Sugar", "SKU", "Bag", 100L, BigDecimal.ONE, BigDecimal.ONE);
        Pageable pageable = PageRequest.of(0, 10);

        when(salesOrdersRepository.pageSupplierPortalProductDistributionV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.singletonList(currentRow), pageable, 1));
        when(salesOrdersRepository.listSupplierPortalProductDistributionV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(previousRow));

        Page<ProductDistributionV2RowDTO> page = service.getProductDistributionV2(
                List.of(), List.of(), start, end, principal, pageable);

        ProductDistributionV2TrendsDTO trends = page.getContent().get(0).getPeriodTrends();
        assertEquals(20.0d, trends.getUnitsSoldTrendPercentage(), 0.0001d);
        assertEquals("vs Previous Period", trends.getTrendLabel());
    }

    @Test
    void getProductDistributionV2_usesListQueryForPreviousWindowTimestamps() {
        ArgumentCaptor<LocalDateTime> currentFrom = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> currentTo = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> previousFrom = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> previousTo = ArgumentCaptor.forClass(LocalDateTime.class);
        Pageable pageable = PageRequest.of(0, 10);

        when(salesOrdersRepository.pageSupplierPortalProductDistributionV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                currentFrom.capture(), currentTo.capture(), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
        when(salesOrdersRepository.listSupplierPortalProductDistributionV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                previousFrom.capture(), previousTo.capture()))
                .thenReturn(Collections.emptyList());

        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);
        service.getProductDistributionV2(List.of(), List.of(), start, end, principal, pageable);

        assertEquals(start.atStartOfDay(), currentFrom.getValue());
        assertEquals(end.atTime(23, 59, 59, 999_999_999), currentTo.getValue());
        assertEquals(LocalDate.of(2026, 3, 3).atStartOfDay(), previousFrom.getValue());
        assertEquals(LocalDate.of(2026, 3, 9).atTime(23, 59, 59, 999_999_999), previousTo.getValue());
    }

    @Test
    void getDistrictPerformanceV2_noFiltersUsesRegionQueries() {
        UUID regionId = UUID.randomUUID();
        Object[] row = new Object[]{
                regionId,
                "Dar es Salaam",
                new BigDecimal("900000"),
                new BigDecimal("12.25"),
                8L,
                15L
        };
        Page<Object[]> currentPage = new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 10), 1);
        when(salesOrdersRepository.pageSupplierPortalDistrictPerformanceByRegionV2(
                eq(SUPPLIER_ID), eq(null), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(currentPage);
        when(salesOrdersRepository.listSupplierPortalDistrictPerformanceByRegionV2(
                eq(SUPPLIER_ID), eq(null), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("567000"));

        DistrictPerformanceV2PageDTO result = service.getDistrictPerformanceV2(
                List.of(), List.of(), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), principal, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        DistrictPerformanceV2RowDTO dto = result.getContent().get(0);
        assertEquals(regionId, dto.getOrganizationLocationPublicId());
        assertEquals("Dar es Salaam", dto.getOrganizationLocationName());
        assertNotNull(dto.getPeriodTrends());
        assertEquals(0, new BigDecimal("567000").compareTo(result.getTotalSupplierLineRevenueTzs()));

        verify(salesOrdersRepository).pageSupplierPortalDistrictPerformanceByRegionV2(
                eq(SUPPLIER_ID), eq(null), any(LocalDateTime.class), any(LocalDateTime.class), eq(PageRequest.of(0, 10)));
        verify(salesOrdersRepository).listSupplierPortalDistrictPerformanceByRegionV2(
                eq(SUPPLIER_ID), eq(null), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, never()).pageSupplierPortalAllProductsWardPerformanceV2(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any(), any());
    }

    @Test
    void getDistrictPerformanceV2_regionIdOnlyUsesWardQueries() {
        UUID regionId = UUID.randomUUID();
        when(salesOrdersRepository.pageSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), any(), eq(true),
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(salesOrdersRepository.listSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), any(), eq(true),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        service.getDistrictPerformanceV2(
                regionId, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), principal, PageRequest.of(0, 10));

        verify(salesOrdersRepository).pageSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), any(), eq(true),
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class));
        verify(salesOrdersRepository, never()).pageSupplierPortalDistrictPerformanceByRegionV2(
                any(), any(), any(), any(), any());
        verify(salesOrdersRepository, never()).listSupplierPortalDistrictPerformanceByRegionV2(
                any(), any(), any(), any());
    }

    @Test
    void getDistrictPerformanceV2_locationIdOnlyUsesWardQueries() {
        UUID locationId = UUID.randomUUID();
        when(salesOrdersRepository.pageSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), any(), eq(true), eq(List.of(locationId)), eq(false),
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(salesOrdersRepository.listSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), any(), eq(true), eq(List.of(locationId)), eq(false),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        service.getDistrictPerformanceV2(
                null, locationId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), principal, PageRequest.of(0, 10));

        verify(salesOrdersRepository).pageSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), any(), eq(true), eq(List.of(locationId)), eq(false),
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class));
        verify(salesOrdersRepository, never()).pageSupplierPortalDistrictPerformanceByRegionV2(
                any(), any(), any(), any(), any());
    }

    @Test
    void getDistrictPerformanceV2_regionAndLocationIdUsesWardQueries() {
        UUID regionId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(salesOrdersRepository.pageSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), eq(List.of(locationId)), eq(false),
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(salesOrdersRepository.listSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), eq(List.of(locationId)), eq(false),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        service.getDistrictPerformanceV2(
                regionId, locationId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), principal, PageRequest.of(0, 10));

        verify(salesOrdersRepository).pageSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), eq(List.of(locationId)), eq(false),
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class));
        verify(salesOrdersRepository, never()).pageSupplierPortalDistrictPerformanceByRegionV2(
                any(), any(), any(), any(), any());
    }

    @Test
    void getDistrictPerformanceV2_mergesPreviousWindowByRegionKeyInDistrictMode() {
        UUID regionId = UUID.randomUUID();
        Object[] currentRow = new Object[]{
                regionId, "Dar es Salaam", new BigDecimal("120"), new BigDecimal("10"), 4L, 6L
        };
        Object[] previousRow = new Object[]{
                regionId, "Dar es Salaam", new BigDecimal("100"), new BigDecimal("8"), 2L, 5L
        };
        when(salesOrdersRepository.pageSupplierPortalDistrictPerformanceByRegionV2(
                eq(SUPPLIER_ID), any(), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(currentRow)));
        when(salesOrdersRepository.listSupplierPortalDistrictPerformanceByRegionV2(
                eq(SUPPLIER_ID), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(previousRow));

        DistrictPerformanceV2PageDTO result = service.getDistrictPerformanceV2(
                List.of(), List.of(), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 16), principal, PageRequest.of(0, 10));

        DistrictPerformanceV2TrendsDTO trends = result.getContent().get(0).getPeriodTrends();
        assertEquals(20.0d, trends.getSupplierLineRevenueTzsTrendPercentage(), 0.0001d);
        assertEquals(25.0d, trends.getSettledTonnesTrendPercentage(), 0.0001d);
        assertEquals(100.0d, trends.getDistinctCustomerCountTrendPercentage(), 0.0001d);
        assertEquals(20.0d, trends.getDeliveredOrderCountTrendPercentage(), 0.0001d);
    }

    @Test
    void getDistrictPerformanceV2_mergesPreviousWindowByWardKeyInWardMode() {
        UUID regionId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        Object[] currentRow = new Object[]{
                wardId, "Kinondoni", new BigDecimal("120"), new BigDecimal("10"), 4L, 6L
        };
        Object[] previousRow = new Object[]{
                wardId, "Kinondoni", new BigDecimal("100"), new BigDecimal("8"), 2L, 5L
        };
        when(salesOrdersRepository.pageSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), any(), eq(true),
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(currentRow)));
        when(salesOrdersRepository.listSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), any(), eq(true),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(previousRow));

        DistrictPerformanceV2PageDTO result = service.getDistrictPerformanceV2(
                regionId, null, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 16), principal, PageRequest.of(0, 10));

        DistrictPerformanceV2TrendsDTO trends = result.getContent().get(0).getPeriodTrends();
        assertEquals(20.0d, trends.getSupplierLineRevenueTzsTrendPercentage(), 0.0001d);
        assertEquals(wardId, result.getContent().get(0).getOrganizationLocationPublicId());
        verify(salesOrdersRepository, never()).pageSupplierPortalDistrictPerformanceByRegionV2(
                any(), any(), any(), any(), any());
    }

    @Test
    void getDistrictPerformanceV2_unknownBucketMergesOnDisplayNameWhenIdNull() {
        Object[] currentRow = new Object[]{null, "UNKNOWN", new BigDecimal("50"), new BigDecimal("1"), 1L, 1L};
        Object[] previousRow = new Object[]{null, "UNKNOWN", new BigDecimal("25"), new BigDecimal("1"), 1L, 1L};
        when(salesOrdersRepository.pageSupplierPortalDistrictPerformanceByRegionV2(
                eq(SUPPLIER_ID), any(), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(currentRow)));
        when(salesOrdersRepository.listSupplierPortalDistrictPerformanceByRegionV2(
                eq(SUPPLIER_ID), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(previousRow));

        DistrictPerformanceV2PageDTO result = service.getDistrictPerformanceV2(
                List.of(), List.of(), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10), principal, PageRequest.of(0, 10));

        assertEquals(100.0d, result.getContent().get(0).getPeriodTrends().getSupplierLineRevenueTzsTrendPercentage(), 0.0001d);
        assertEquals("UNKNOWN:" + "UNKNOWN",
                SupplierPortalDashboardServiceImpl.districtMergeKey(result.getContent().get(0)));
    }

    @Test
    void getDashboardSummaryMetrics_passesLocationIdToGeographyQueries() {
        UUID locationId = UUID.randomUUID();
        stubDashboardSummaryMetrics(1L, 1L, BigDecimal.ONE, BigDecimal.TEN);

        service.getDashboardSummaryMetrics(null, locationId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), principal);

        verify(salesOrdersRepository, times(2)).countDistinctDeliveredSupplierPortalCustomers(
                eq(SUPPLIER_ID), any(), eq(true), eq(List.of(locationId)), eq(false), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, times(2)).countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), eq(true), eq(List.of(locationId)), eq(false), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, times(2)).sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), eq(true), eq(List.of(locationId)), eq(false), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, times(2)).sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), eq(true), eq(List.of(locationId)), eq(false), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getDashboardSummaryMetrics_usesGeographyQueriesAndZeroTrendWhenPreviousBaselineZero() {
        stubDashboardSummaryMetrics(
                5L, 20L, new BigDecimal("10"), new BigDecimal("1000"),
                0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);

        SummaryCardsV2DTO summary = service.getDashboardSummaryMetrics(
                List.of(), List.of(), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 16), principal);

        assertEquals(20L, summary.getTotalOrders().getValue());
        assertEquals(0.0d, summary.getTotalOrders().getTrendPercentage(), 0.0001d);
        assertEquals(0.0d, summary.getActiveCustomers().getTrendPercentage(), 0.0001d);
        assertEquals(0.0d, summary.getTotalQuantitySoldTonnes().getTrendPercentage(), 0.0001d);
        assertEquals(0.0d, summary.getTotalCogsTzs().getTrendPercentage(), 0.0001d);
        assertEquals("vs Previous Period", summary.getTotalOrders().getTrendLabel());
        assertEquals(5L, summary.getActiveCustomers().getValue());

        verify(salesOrdersRepository, never()).countActiveCustomers(any(), any(), any(), any());
        verify(salesOrdersRepository, never()).totalWeightDelivered(any(), any(), any(), any());
        verify(salesOrdersRepository, never()).aggregateSupplierPortalDashboardSummaryMetricsV2(
                any(), any(), any(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, times(2)).countDistinctDeliveredSupplierPortalCustomers(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
        verify(salesOrdersRepository, times(2)).countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
        verify(salesOrdersRepository, never()).sumWeightDeliveredTonnesBySupplierRegionAndLocation(
                any(), any(), any(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, times(2)).sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
        verify(salesOrdersRepository, times(2)).sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getSalesTrend_yearOnly_usesTwelveMonthlyBucketsAndLastYearHeadline() {
        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(
                        new Object[]{new BigDecimal("100000"), 2025, 2},
                        new Object[]{new BigDecimal("150000"), 2025, 3}));
        // year-only: currentTotal is derived from the monthly-series sum (100k+150k=250k);
        // only one repo call is made for the previous-year baseline
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("200000"));

        SalesTrendV2DTO trend = service.getSalesTrend(
                null, null, null, 2025, null, false, false, principal, PageRequest.of(0, 100));

        assertEquals(12, trend.getSeries().size());
        assertEquals(2025, trend.getSeries().get(0).getYear());
        assertEquals(1, trend.getSeries().get(0).getMonth());
        assertEquals(0, new BigDecimal("250000").compareTo(trend.getMonthToDateCogsVersusLastYear().getValue()));
        assertEquals("vs Last Year", trend.getMonthToDateCogsVersusLastYear().getTrendLabel());
        assertEquals(25.0d, trend.getMonthToDateCogsVersusLastYear().getTrendPercentage(), 0.0001d);

        verify(salesOrdersRepository).aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getSalesTrend_rejectsLegacyGranularityFlags() {
        assertThrows(BadRequestRestApiException.class, () -> service.getSalesTrend(
                null, null, null, 2025, true, false, false, principal, PageRequest.of(0, 100)));

        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getSalesTrend_withMonthYear_usesDailySeriesAndPreviousPeriodHeadline() {
        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> prevFromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> prevToCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        Object[] dayWithRevenue = {new BigDecimal("150000"), Date.valueOf(LocalDate.of(2025, 3, 15))};
        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), fromCaptor.capture(), toCaptor.capture()))
                .thenReturn(Collections.singletonList(dayWithRevenue));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), prevFromCaptor.capture(), prevToCaptor.capture()))
                .thenReturn(new BigDecimal("120000"));

        SalesTrendV2DTO trend = service.getSalesTrend(
                null, null, 3, 2025, null, false, false, principal, PageRequest.of(0, 100));

        assertEquals(31, trend.getSeries().size());
        assertEquals(LocalDate.of(2025, 3, 1).atStartOfDay(), fromCaptor.getValue());
        assertEquals(LocalDate.of(2025, 3, 31), toCaptor.getValue().toLocalDate());
        assertEquals(0, new BigDecimal("150000").compareTo(trend.getMonthToDateCogsVersusLastYear().getValue()));
        assertEquals("vs Previous Period", trend.getMonthToDateCogsVersusLastYear().getTrendLabel());
        assertEquals(LocalDate.of(2025, 2, 1).atStartOfDay(), prevFromCaptor.getValue());
        assertEquals(LocalDate.of(2025, 2, 28), prevToCaptor.getValue().toLocalDate());

        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getSalesTrend_rejectsMonthWithoutYear() {
        assertThrows(BadRequestRestApiException.class, () -> service.getSalesTrend(
                null, null, 3, null, null, false, false, principal, PageRequest.of(0, 100)));

        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getSalesTrend_rejectsFutureMonthYear() {
        assertThrows(BadRequestRestApiException.class, () -> service.getSalesTrend(
                null, null, 12, 2099, null, false, false, principal, PageRequest.of(0, 100)));

        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getProductDistributionV2_usesEndOfDayInclusiveUpperBound() {
        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        Pageable pageable = PageRequest.of(0, 10);
        when(salesOrdersRepository.pageSupplierPortalProductDistributionV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                fromCaptor.capture(), toCaptor.capture(), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
        when(salesOrdersRepository.listSupplierPortalProductDistributionV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        service.getProductDistributionV2(List.of(), List.of(), start, end, principal, pageable);

        assertEquals(start.atStartOfDay(), fromCaptor.getValue());
        assertEquals(end, toCaptor.getValue().toLocalDate());
        assertEquals(LocalTime.MAX, toCaptor.getValue().toLocalTime());
    }

    @Test
    void getProductDistributionV2_rejectsInvalidDateRange() {
        LocalDate end = LocalDate.of(2026, 3, 10);

        assertThrows(BadRequestRestApiException.class, () -> service.getProductDistributionV2(
                List.of(), List.of(), LocalDate.of(2026, 3, 20), end, principal, PageRequest.of(0, 10)));
        assertThrows(BadRequestRestApiException.class, () -> service.getProductDistributionV2(
                List.of(), List.of(), null, end, principal, PageRequest.of(0, 10)));

        verify(salesOrdersRepository, never()).pageSupplierPortalProductDistributionV2(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any(), any());
        verify(salesOrdersRepository, never()).listSupplierPortalProductDistributionV2(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getSalesCalendar_zeroFillsEveryDayAndBuildsSummary() {
        Object[] busyDay = salesCalendarRow(
                Date.valueOf("2026-05-10"),
                new BigDecimal("200000"),
                new BigDecimal("4.5"),
                3L);
        when(salesOrdersRepository.aggregateSupplierPortalSalesCalendarByDay(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(busyDay));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("150000"));
        when(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2L);
        when(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("3.0"));

        SalesCalendarV2DTO calendar = service.getSalesCalendar(List.of(), List.of(), 5, 2026, principal);

        assertEquals(5, calendar.getMonth());
        assertEquals(2026, calendar.getYear());
        assertEquals("May 2026", calendar.getMonthLabel());
        assertEquals(31, calendar.getDays().size());
        assertEquals(LocalDate.of(2026, 5, 1), calendar.getDays().get(0).getDate());
        assertEquals(LocalDate.of(2026, 5, 31), calendar.getDays().get(30).getDate());

        SalesCalendarDayV2DTO peak = calendar.getDays().get(9);
        assertEquals(LocalDate.of(2026, 5, 10), peak.getDate());
        assertEquals(0, new BigDecimal("200000").compareTo(peak.getRevenueTzs()));
        assertEquals(1.0d, peak.getIntensity(), 0.0001d);
        assertEquals("PEAK", peak.getIntensityLevel());
        assertEquals("TZS 200,000", peak.getRevenueLabel());
        assertEquals("4.5 Tonnes", peak.getQuantitySoldLabel());
        assertEquals(3L, peak.getOrderCount());

        SalesCalendarDayV2DTO quiet = calendar.getDays().get(0);
        assertEquals(0.0d, quiet.getIntensity(), 0.0001d);
        assertEquals("NONE", quiet.getIntensityLevel());
        assertEquals(0L, quiet.getOrderCount());

        assertEquals(LocalDate.of(2026, 5, 10), calendar.getSummary().getPeakDay());
        assertEquals(0, new BigDecimal("4.5").compareTo(calendar.getSummary().getUnitsSoldTonnes()));
        assertEquals(3L, calendar.getSummary().getTotalOrders());
        assertEquals("vs Previous Month", calendar.getSummary().getMonthlyRevenueVersusPreviousMonth().getTrendLabel());
        assertNotNull(calendar.getSummary().getMonthlyUnitsSoldVersusPreviousMonth());
        assertEquals(0, new BigDecimal("4.5").compareTo(calendar.getSummary().getMonthlyUnitsSoldVersusPreviousMonth().getValue()));
        assertEquals("vs Previous Month", calendar.getSummary().getMonthlyUnitsSoldVersusPreviousMonth().getTrendLabel());
        assertEquals(50.0d, calendar.getSummary().getMonthlyUnitsSoldVersusPreviousMonth().getTrendPercentage(), 0.0001d);
    }

    @Test
    void getSalesCalendar_handlesTimestampAndStringDayColumns() {
        when(salesOrdersRepository.aggregateSupplierPortalSalesCalendarByDay(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(
                        salesCalendarRow(Date.valueOf("2026-05-10"), new BigDecimal("100"), BigDecimal.ONE, 1L),
                        new Object[]{
                                java.sql.Timestamp.valueOf("2026-05-11 00:00:00"),
                                new BigDecimal("200"),
                                BigDecimal.ONE,
                                2L
                        },
                        new Object[]{"2026-05-12", new BigDecimal("300"), BigDecimal.ONE, 3L}));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);
        when(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        SalesCalendarV2DTO calendar = service.getSalesCalendar(List.of(), List.of(), 5, 2026, principal);

        assertEquals(LocalDate.of(2026, 5, 10), calendar.getDays().get(9).getDate());
        assertEquals(LocalDate.of(2026, 5, 11), calendar.getDays().get(10).getDate());
        assertEquals(LocalDate.of(2026, 5, 12), calendar.getDays().get(11).getDate());
    }

    @Test
    void getSalesCalendar_rejectsInvalidMonth() {
        assertThrows(BadRequestRestApiException.class, () -> service.getSalesCalendar(List.of(), List.of(), 13, 2026, principal));
        verify(salesOrdersRepository, never()).aggregateSupplierPortalSalesCalendarByDay(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    private void stubDashboardSummaryMetrics(long currentCustomers,
                                             long currentOrders,
                                             BigDecimal currentTonnes,
                                             BigDecimal currentCogs) {
        stubDashboardSummaryMetrics(
                currentCustomers, currentOrders, currentTonnes, currentCogs,
                currentCustomers, currentOrders, currentTonnes, currentCogs);
    }

    private void stubDashboardSummaryMetrics(long currentCustomers,
                                             long currentOrders,
                                             BigDecimal currentTonnes,
                                             BigDecimal currentCogs,
                                             long previousCustomers,
                                             long previousOrders,
                                             BigDecimal previousTonnes,
                                             BigDecimal previousCogs) {
        when(salesOrdersRepository.countDistinctDeliveredSupplierPortalCustomers(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(currentCustomers, previousCustomers);
        when(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(currentOrders, previousOrders);
        when(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(currentTonnes, previousTonnes);
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(currentCogs, previousCogs);
    }

    private static Object[] productDistributionRow(
            UUID productId,
            String name,
            String sku,
            String uom,
            long unitsSold,
            BigDecimal revenue,
            BigDecimal tonnes) {
        return new Object[]{productId, name, sku, uom, unitsSold, revenue, tonnes};
    }

    private static Object[] salesCalendarRow(Date date, BigDecimal revenue, BigDecimal tonnes, long orderCount) {
        return new Object[]{date, revenue, tonnes, orderCount};
    }
}
