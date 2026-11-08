package com.app.itibari.services;

import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.report.ProductAnalysisDistrictCogsV2DTO;
import com.app.itibari.models.report.ProductAnalysisDistrictRankingV2DTO;
import com.app.itibari.models.report.ProductAnalysisProductRankingCogsRowV2DTO;
import com.app.itibari.models.report.ProductAnalysisWardPeriodTrendsDTO;
import com.app.itibari.models.report.ProductAnalysisSalesCalendarV2DTO;
import com.app.itibari.models.report.ProductAnalysisSalesTrendV2DTO;
import com.app.itibari.models.report.ProductAnalysisSummaryMetricsV2DTO;
import com.app.itibari.models.report.ProductAnalysisWardPerformanceRowDTO;
import com.app.itibari.models.response.FileResponseDTO;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.services.helper.ProductImageResolver;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierPortalProductAnalysisAllProductsServiceImplTest {

    private static final UUID SUPPLIER_ID = UUID.fromString("30577548-e687-4115-9d51-3a33a363aa56");
    private static final String SUPPLIER_EMAIL = "supp@email.com";

    private SalesOrdersRepository salesOrdersRepository;
    private PersonRepository personRepository;
    private SupplierRepository supplierRepository;
    private ProductRepository productRepository;
    private FileService fileService;
    private ProductImageResolver productImageResolver;
    private Principal principal;
    private SupplierPortalProductAnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        salesOrdersRepository = mock(SalesOrdersRepository.class);
        personRepository = mock(PersonRepository.class);
        supplierRepository = mock(SupplierRepository.class);
        productRepository = mock(ProductRepository.class);
        fileService = mock(FileService.class);
        productImageResolver = new ProductImageResolver(fileService);
        principal = mock(Principal.class);
        service = new SupplierPortalProductAnalysisServiceImpl(
                salesOrdersRepository, personRepository, supplierRepository, productRepository, productImageResolver);
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
    void getAllProductsSummaryMetrics_mapsSupplierWideTrendMetrics() throws Exception {
        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);

        when(salesOrdersRepository.countDistinctDeliveredSupplierPortalCustomers(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(12L, 6L);
        when(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(30L, 15L);
        when(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("25.5"), new BigDecimal("20"));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("2000000"), new BigDecimal("1600000"));

        ProductAnalysisSummaryMetricsV2DTO dto = service.getAllProductsSummaryMetrics(
                null, null, start, end, principal);

        assertEquals(12L, dto.getActiveCustomers().getValue());
        assertEquals(100d, dto.getActiveCustomers().getTrendPercentage());
        assertEquals(30L, dto.getTotalOrders().getValue());
        assertEquals(0, new BigDecimal("25.5").compareTo(dto.getTotalQuantitySoldTonnes().getValue()));
        assertEquals(0, new BigDecimal("2000000").compareTo(dto.getTotalCogsTzs().getValue()));
        assertEquals(0, new BigDecimal("66666.67").compareTo(dto.getAverageOrderValueTzs().getValue()));
        assertEquals(-37.5d, dto.getAverageOrderValueTzs().getTrendPercentage());
        assertNull(dto.getProductImageUrl());
        assertNull(dto.getRanking());

        verify(productRepository, never()).existsByPublicIdAndSupplierPublicId(any(), any());
        verify(fileService, never()).getImages(any(), any());
    }

    @Test
    void getAllProductsSummaryMetrics_aovZeroWhenNoDeliveredOrders() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        when(salesOrdersRepository.countDistinctDeliveredSupplierPortalCustomers(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);
        when(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);
        when(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);

        ProductAnalysisSummaryMetricsV2DTO dto = service.getAllProductsSummaryMetrics(
                null, null, start, end, principal);

        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(dto.getAverageOrderValueTzs().getValue()));
        assertNull(dto.getAverageOrderValueTzs().getTrendPercentage());
    }

    @Test
    void getAllProductsSalesTrend_zeroFillsDailySeriesAndHeadline() {
        Object[] dayTwo = {
                new BigDecimal("800"),
                Date.valueOf(LocalDate.of(2026, 3, 2)),
                12L,
                7L
        };

        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(dayTwo));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("800"), BigDecimal.ZERO);

        ProductAnalysisSalesTrendV2DTO dto = service.getAllProductsSalesTrend(
                null, null, 3, 2026, principal);

        assertEquals(31, dto.getSeries().size());
        assertEquals(LocalDate.of(2026, 3, 1), dto.getSeries().get(0).getDate());
        assertEquals(LocalDate.of(2026, 3, 31), dto.getSeries().get(30).getDate());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getSeries().get(0).getTotalCogsTzs()));
        assertEquals(0L, dto.getSeries().get(0).getOrderCount());
        assertEquals(0L, dto.getSeries().get(0).getCustomerCount());
        assertEquals(0, new BigDecimal("800").compareTo(dto.getSeries().get(1).getTotalCogsTzs()));
        assertEquals(12L, dto.getSeries().get(1).getOrderCount());
        assertEquals(7L, dto.getSeries().get(1).getCustomerCount());
        assertEquals(0, new BigDecimal("800").compareTo(dto.getHeadline().getValue()));
    }

    @Test
    void getAllProductsSalesTrend_resolvesMonthYearToFullCalendarWindow() {
        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);

        service.getAllProductsSalesTrend(null, null, 3, 2026, principal);

        org.mockito.ArgumentCaptor<LocalDateTime> fromCaptor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        org.mockito.ArgumentCaptor<LocalDateTime> toCaptor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(salesOrdersRepository).aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), fromCaptor.capture(), toCaptor.capture());
        assertEquals(LocalDate.of(2026, 3, 1).atStartOfDay(), fromCaptor.getValue());
        assertEquals(LocalDate.of(2026, 3, 31).atTime(23, 59, 59, 999_999_999), toCaptor.getValue());
    }

    @Test
    void getAllProductsSalesTrend_rejectsMonthWithoutYear() {
        assertThrows(BadRequestRestApiException.class, () -> service.getAllProductsSalesTrend(
                null, null, 3, null, principal));

        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getAllProductsSalesTrend_yearOnly_usesTwelveMonthlyBucketsAndLastYearHeadline() {
        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(
                        new Object[]{new BigDecimal("100"), 2025, 1, 2L, 1L},
                        new Object[]{new BigDecimal("200"), 2025, 2, 3L, 2L}));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("300"), new BigDecimal("250"));

        ProductAnalysisSalesTrendV2DTO dto = service.getAllProductsSalesTrend(
                null, null, null, 2025, principal);

        assertEquals(12, dto.getSeries().size());
        assertEquals(LocalDate.of(2025, 1, 1), dto.getSeries().get(0).getDate());
        assertEquals(LocalDate.of(2025, 12, 1), dto.getSeries().get(11).getDate());
        assertEquals(0, new BigDecimal("300").compareTo(dto.getHeadline().getValue()));
        assertEquals(
                dto.getSeries().stream()
                        .map(com.app.itibari.models.report.ProductAnalysisDailyCogsPointDTO::getTotalCogsTzs)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                dto.getHeadline().getValue());
        assertEquals("vs Last Year", dto.getHeadline().getTrendLabel());
        assertEquals(0, new BigDecimal("100").compareTo(dto.getSeries().get(0).getTotalCogsTzs()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getSeries().get(2).getTotalCogsTzs()));
        assertEquals(2L, dto.getSeries().get(0).getOrderCount());
        assertEquals(1L, dto.getSeries().get(0).getCustomerCount());

        verify(salesOrdersRepository).aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationMonthForProduct(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getAllProductsDistrictRanking_noFilters_usesRegionRankingWithTrends() {
        UUID regionA = UUID.randomUUID();
        UUID regionB = UUID.randomUUID();
        Object[] first = {regionA, "Dar es Salaam", new BigDecimal("10"), new BigDecimal("100000"), 4L};
        Object[] second = {regionB, "Arusha", new BigDecimal("5"), new BigDecimal("50000"), 2L};
        Object[] previousFirst = {regionA, "Dar es Salaam", new BigDecimal("5"), new BigDecimal("40000"), 2L};

        when(salesOrdersRepository.listSupplierPortalAllProductsRegionRankingBySettledTonnes(
                eq(SUPPLIER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(first, second), Collections.singletonList(previousFirst));
        when(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("15"));

        ProductAnalysisDistrictRankingV2DTO dto = service.getAllProductsDistrictRanking(
                null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 1, principal);

        assertEquals(1, dto.getRows().size());
        assertEquals(1, dto.getRows().get(0).getRank());
        assertEquals("Dar es Salaam", dto.getRows().get(0).getOrganizationLocationName());
        assertEquals(0, new BigDecimal("10").compareTo(dto.getRows().get(0).getSettledTonnes()));
        assertEquals(0, new BigDecimal("25000.00").compareTo(dto.getRows().get(0).getAverageOrderValueTzs()));
        assertEquals(100d, dto.getRows().get(0).getPeriodTrends().getSettledTonnesTrendPercentage());
        assertEquals(0, new BigDecimal("15").compareTo(dto.getTotalSettledTonnes()));
        verify(salesOrdersRepository, org.mockito.Mockito.times(2))
                .listSupplierPortalAllProductsRegionRankingBySettledTonnes(
                        eq(SUPPLIER_ID), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, never()).listSupplierPortalAllProductsWardRankingBySettledTonnes(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getAllProductsDistrictRanking_regionFilter_usesWardRankingUnchanged() {
        UUID regionId = UUID.randomUUID();
        UUID wardA = UUID.randomUUID();
        Object[] first = {wardA, "Kinondoni", new BigDecimal("10"), new BigDecimal("100000"), 4L};

        when(salesOrdersRepository.listSupplierPortalAllProductsWardRankingBySettledTonnes(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), any(), eq(true),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(first), Collections.emptyList());
        when(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), any(), eq(true), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("10"));

        ProductAnalysisDistrictRankingV2DTO dto = service.getAllProductsDistrictRanking(
                List.of(regionId), null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 10, principal);

        assertEquals("Kinondoni", dto.getRows().get(0).getOrganizationLocationName());
        verify(salesOrdersRepository, never()).listSupplierPortalAllProductsRegionRankingBySettledTonnes(
                any(), any(), any());
    }

    @Test
    void getAllProductsDistrictRanking_noFilters_handlesUnknownRegionName() {
        Object[] row = {null, null, new BigDecimal("3"), BigDecimal.ZERO, 0L};

        when(salesOrdersRepository.listSupplierPortalAllProductsRegionRankingBySettledTonnes(
                eq(SUPPLIER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(row), Collections.emptyList());
        when(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("3"));

        ProductAnalysisDistrictRankingV2DTO dto = service.getAllProductsDistrictRanking(
                null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 10, principal);

        assertEquals("UNKNOWN", dto.getRows().get(0).getOrganizationLocationName());
        assertEquals(1, dto.getRows().get(0).getRank());
    }

    @Test
    void getAllProductsDistrictCogs_noFilters_usesRegionCogsAndMergesTrendsByRegionKey() {
        UUID regionA = UUID.randomUUID();
        UUID regionB = UUID.randomUUID();
        Object[] first = {regionA, "Dar es Salaam", new BigDecimal("900000")};
        Object[] second = {regionB, "Arusha", new BigDecimal("500000")};
        Object[] previousFirst = {regionA, "Dar es Salaam", new BigDecimal("450000")};

        when(salesOrdersRepository.listSupplierPortalAllProductsRegionCogsByTotalCogs(
                eq(SUPPLIER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(first, second), Collections.singletonList(previousFirst));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1400000"));

        ProductAnalysisDistrictCogsV2DTO dto = service.getAllProductsDistrictCogs(
                null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 1, principal);

        assertEquals(1, dto.getRows().size());
        assertEquals(1, dto.getRows().get(0).getRank());
        assertEquals("Dar es Salaam", dto.getRows().get(0).getOrganizationLocationName());
        assertEquals(0, new BigDecimal("1400000").compareTo(dto.getTotalCogsTzs()));
        assertEquals(100d, dto.getRows().get(0).getPeriodTrends().getTotalCogsTzsTrendPercentage());
        verify(salesOrdersRepository, org.mockito.Mockito.times(2))
                .listSupplierPortalAllProductsRegionCogsByTotalCogs(
                        eq(SUPPLIER_ID), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, never()).listSupplierPortalAllProductsWardCogsByTotalCogs(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getAllProductsDistrictCogs_regionFilter_usesWardCogsUnchanged() {
        UUID regionId = UUID.randomUUID();
        UUID wardA = UUID.randomUUID();
        Object[] first = {wardA, "Kinondoni", new BigDecimal("900000")};

        when(salesOrdersRepository.listSupplierPortalAllProductsWardCogsByTotalCogs(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), any(), eq(true), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(first), Collections.emptyList());
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), eq(List.of(regionId)), eq(false), any(), eq(true), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("900000"));

        ProductAnalysisDistrictCogsV2DTO dto = service.getAllProductsDistrictCogs(
                List.of(regionId), null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 10, principal);

        assertEquals("Kinondoni", dto.getRows().get(0).getOrganizationLocationName());
        verify(salesOrdersRepository, never()).listSupplierPortalAllProductsRegionCogsByTotalCogs(
                any(), any(), any());
    }

    @Test
    void getAllProductsProductRankingCogs_mapsProductRowsRanksByCogsAndEnrichesImage() throws IOException {
        UUID productId = UUID.randomUUID();
        PageRequest uiPageable = PageRequest.of(0, 10);
        Object[] row = productRankingCogsRow(
                productId,
                "Sugar 50kg",
                120L,
                new BigDecimal("500000"));

        when(salesOrdersRepository.pageSupplierPortalAllProductsProductRankingByCogs(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class), eq(uiPageable)))
                .thenReturn(new PageImpl<>(Collections.singletonList(row), uiPageable, 25));
        when(salesOrdersRepository.listSupplierPortalAllProductsProductRankingByCogs(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(fileService.getImages(productId, ReferenceType.PRODUCT))
                .thenReturn(new FileResponseDTO("https://cdn.example/sugar.png", "image/png", null, null, true));

        Page<ProductAnalysisProductRankingCogsRowV2DTO> page = service.getAllProductsProductRankingCogs(
                null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), principal, PageRequest.of(1, 50));

        assertEquals(1, page.getContent().size());
        ProductAnalysisProductRankingCogsRowV2DTO dto = page.getContent().get(0);
        assertEquals(1, dto.getRank());
        assertEquals(productId, dto.getProductPublicId());
        assertEquals("Sugar 50kg", dto.getProductName());
        assertEquals("Bag", dto.getUnitOfMeasure());
        assertEquals(0, new BigDecimal("500000").compareTo(dto.getTotalCogsTzs()));
        assertEquals("TZS 500,000", dto.getTotalCogsTzsLabel());
        assertEquals(0, BigDecimal.valueOf(120L).compareTo(dto.getTotalQuantitySoldUnits()));
        assertEquals("https://cdn.example/sugar.png", dto.getProductImageUrl());
        assertEquals(10, page.getTotalElements());
        assertTrue(page.isLast());
        assertNull(dto.getPeriodTrends().getTotalCogsTzsTrendPercentage());

        verify(salesOrdersRepository).pageSupplierPortalAllProductsProductRankingByCogs(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class), eq(uiPageable));
        verify(salesOrdersRepository).listSupplierPortalAllProductsProductRankingByCogs(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, never()).listSupplierPortalAllProductsWardCogsByTotalCogs(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getAllProductsProductRankingCogs_mergesPreviousWindowByProductId() {
        UUID productId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);
        Pageable pageable = PageRequest.of(0, 10);
        Object[] currentRow = productRankingCogsRow(productId, "Sugar 50kg", 120L, new BigDecimal("500000"));
        Object[] previousRow = productRankingCogsRow(productId, "Sugar 50kg", 100L, new BigDecimal("250000"));

        when(salesOrdersRepository.pageSupplierPortalAllProductsProductRankingByCogs(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.singletonList(currentRow), pageable, 1));
        when(salesOrdersRepository.listSupplierPortalAllProductsProductRankingByCogs(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(previousRow));

        Page<ProductAnalysisProductRankingCogsRowV2DTO> page = service.getAllProductsProductRankingCogs(
                null, null, start, end, principal, pageable);

        ProductAnalysisWardPeriodTrendsDTO trends = page.getContent().get(0).getPeriodTrends();
        assertEquals(100d, trends.getTotalCogsTzsTrendPercentage());
        assertEquals("vs Previous Period", trends.getTrendLabel());
    }

    @Test
    void getAllProductsProductRankingCogs_usesListQueryForPreviousWindowTimestamps() {
        ArgumentCaptor<LocalDateTime> currentFrom = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> currentTo = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> previousFrom = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> previousTo = ArgumentCaptor.forClass(LocalDateTime.class);
        Pageable pageable = PageRequest.of(0, 10);

        when(salesOrdersRepository.pageSupplierPortalAllProductsProductRankingByCogs(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                currentFrom.capture(), currentTo.capture(), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
        when(salesOrdersRepository.listSupplierPortalAllProductsProductRankingByCogs(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(),
                previousFrom.capture(), previousTo.capture()))
                .thenReturn(Collections.emptyList());

        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);
        service.getAllProductsProductRankingCogs(null, null, start, end, principal, pageable);

        assertEquals(start.atStartOfDay(), currentFrom.getValue());
        assertEquals(end.atTime(23, 59, 59, 999_999_999), currentTo.getValue());
        assertEquals(LocalDate.of(2026, 3, 3).atStartOfDay(), previousFrom.getValue());
        assertEquals(LocalDate.of(2026, 3, 9).atTime(23, 59, 59, 999_999_999), previousTo.getValue());
    }

    @Test
    void getAllProductsProductRankingCogs_rejectsInvalidDateRange() {
        assertThrows(BadRequestRestApiException.class, () -> service.getAllProductsProductRankingCogs(
                null, null, LocalDate.of(2026, 3, 31), LocalDate.of(2026, 3, 1), principal, PageRequest.of(0, 10)));
        verify(salesOrdersRepository, never()).pageSupplierPortalAllProductsProductRankingByCogs(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any(), any());
        verify(salesOrdersRepository, never()).listSupplierPortalAllProductsProductRankingByCogs(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getAllProductsWardPerformance_mapsPagedRows() {
        UUID wardA = UUID.randomUUID();
        Object[] row = {wardA, "Kinondoni", new BigDecimal("1000"), new BigDecimal("2.5"), 5L, 6L};
        PageImpl<Object[]> page = new PageImpl<>(Collections.singletonList(row));

        when(salesOrdersRepository.pageSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(salesOrdersRepository.listSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        org.springframework.data.domain.Page<ProductAnalysisWardPerformanceRowDTO> result =
                service.getAllProductsWardPerformance(
                        null,
                        null,
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 31),
                        principal,
                        PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        ProductAnalysisWardPerformanceRowDTO dto = result.getContent().get(0);
        assertEquals(1, dto.getRank());
        assertEquals(5L, dto.getDistinctCustomerCount());
        assertEquals(6L, dto.getDeliveredOrderCount());
        assertEquals(0, new BigDecimal("166.67").compareTo(dto.getAverageOrderValueTzs()));
        assertNull(dto.getPeriodTrends().getTotalCogsTzsTrendPercentage());
        assertNull(dto.getPeriodTrends().getSettledTonnesTrendPercentage());

        verify(salesOrdersRepository).listSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getAllProductsWardPerformance_mergesPreviousWindowTrendByWardKey() {
        UUID wardA = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);
        Object[] currentRow = {wardA, "Kinondoni", new BigDecimal("1200"), new BigDecimal("3.0"), 5L, 6L};
        Object[] previousRow = {wardA, "Kinondoni", new BigDecimal("600"), new BigDecimal("1.5"), 4L, 3L};

        when(salesOrdersRepository.pageSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(currentRow), PageRequest.of(0, 20), 1));
        when(salesOrdersRepository.listSupplierPortalAllProductsWardPerformanceV2(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(previousRow));

        Page<ProductAnalysisWardPerformanceRowDTO> result = service.getAllProductsWardPerformance(
                null, null, start, end, principal, PageRequest.of(0, 20));

        ProductAnalysisWardPeriodTrendsDTO trends = result.getContent().get(0).getPeriodTrends();
        assertEquals(100d, trends.getTotalCogsTzsTrendPercentage());
        assertEquals(100d, trends.getSettledTonnesTrendPercentage());
        assertEquals(0d, trends.getAverageOrderValueTzsTrendPercentage());
    }

    @Test
    void getAllProductsWardPerformance_rejectsClientSort() {
        org.springframework.data.domain.Pageable sorted = PageRequest.of(
                0, 20, org.springframework.data.domain.Sort.by("settledTonnes"));

        assertThrows(BadRequestRestApiException.class, () -> service.getAllProductsWardPerformance(
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                principal,
                sorted));

        verify(salesOrdersRepository, never()).pageSupplierPortalAllProductsWardPerformanceV2(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any(), any());
    }

    @Test
    void getAllProductsSalesCalendar_zeroFillsAllDaysInMonth() {
        Object[] dayTwo = {new BigDecimal("750"), Date.valueOf(LocalDate.of(2026, 2, 2))};

        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(dayTwo));
        when(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(7L);
        when(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("20"));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("2500"));

        ProductAnalysisSalesCalendarV2DTO dto = service.getAllProductsSalesCalendar(
                null, null, 2, 2026, principal);

        assertEquals(2, dto.getMonth());
        assertEquals(2026, dto.getYear());
        assertEquals(28, dto.getDays().size());
        assertEquals(0, new BigDecimal("750").compareTo(dto.getDays().get(1).getTotalCogsTzs()));
        assertEquals(7L, dto.getTotalOrders());
        assertEquals(0, new BigDecimal("20").compareTo(dto.getTotalQuantitySoldTonnes()));
        assertEquals(0, new BigDecimal("2500").compareTo(dto.getTotalCogsTzs()));
        assertEquals(LocalDate.of(2026, 2, 2), dto.getPeakDay().getDate());
    }

    @Test
    void getAllProductsSalesCalendar_noSalesMonthReturnsNullPeakDayAndZeroSummaries() {
        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        ProductAnalysisSalesCalendarV2DTO dto = service.getAllProductsSalesCalendar(
                null, null, 2, 2024, principal);

        assertNull(dto.getPeakDay());
        assertEquals(29, dto.getDays().size());
        assertEquals(0L, dto.getTotalOrders());
    }

    @Test
    void getAllProductsSalesCalendar_rejectsInvalidMonth() {
        assertThrows(BadRequestRestApiException.class, () -> service.getAllProductsSalesCalendar(
                null, null, 13, 2026, principal));

        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationDayForAllSupplierProducts(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void productDetailAndAllProductsModesUseDifferentRepositoryMethods() {
        when(salesOrdersRepository.countActiveCustomersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any()))
                .thenReturn(1L, 0L);
        when(salesOrdersRepository.countDistinctDeliveredSupplierPortalCustomers(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L, 0L);
        when(salesOrdersRepository.countDeliveredOrdersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any()))
                .thenReturn(1L, 0L);
        when(salesOrdersRepository.countDeliveredOrdersForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L, 0L);
        when(salesOrdersRepository.sumSettledTonnesBySupplierProductAndGeography(
                eq(SUPPLIER_ID), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any()))
                .thenReturn(BigDecimal.ONE, BigDecimal.ZERO);
        when(salesOrdersRepository.sumSettledTonnesForAllSupplierProductsByRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("5"), BigDecimal.ZERO);
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any()))
                .thenReturn(BigDecimal.ONE, BigDecimal.ZERO);
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierRegionAndLocation(
                eq(SUPPLIER_ID), any(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("5"), BigDecimal.ZERO);
        when(productRepository.existsByPublicIdAndSupplierPublicId(any(), eq(SUPPLIER_ID))).thenReturn(true);

        UUID productId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 7);

        ProductAnalysisSummaryMetricsV2DTO productScoped = service.getSummaryMetrics(
                productId, null, null, start, end, principal);
        ProductAnalysisSummaryMetricsV2DTO supplierWide = service.getAllProductsSummaryMetrics(
                null, null, start, end, principal);

        assertEquals(1L, productScoped.getActiveCustomers().getValue());
        assertEquals(5L, supplierWide.getActiveCustomers().getValue());
    }

    private static Object[] productRankingCogsRow(UUID productId,
                                                  String productName,
                                                  long quantityDelivered,
                                                  BigDecimal cogsTzs) {
        return new Object[]{
                productId,
                productName,
                "SKU-1",
                "Bag",
                quantityDelivered,
                cogsTzs
        };
    }
}
