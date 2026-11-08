package com.app.itibari.services;

import com.app.itibari.dataTypes.ReferenceType;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.response.FileResponseDTO;
import com.app.itibari.models.report.ProductAnalysisDistrictCogsV2DTO;
import com.app.itibari.models.report.ProductAnalysisDistrictRankingV2DTO;
import com.app.itibari.models.report.ProductAnalysisSalesCalendarV2DTO;
import com.app.itibari.models.report.ProductAnalysisDailyCogsPointDTO;
import com.app.itibari.models.report.ProductAnalysisSalesTrendV2DTO;
import com.app.itibari.models.report.ProductAnalysisSummaryMetricsV2DTO;
import com.app.itibari.models.report.ProductAnalysisWardPerformanceRowDTO;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.services.helper.ProductImageResolver;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.security.Principal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierPortalProductAnalysisServiceImplTest {

    private static final UUID SUPPLIER_ID = UUID.fromString("30577548-e687-4115-9d51-3a33a363aa56");
    private static final UUID PRODUCT_ID = UUID.fromString("a1706430-1111-4222-8333-000000000001");
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
        when(productRepository.existsByPublicIdAndSupplierPublicId(PRODUCT_ID, SUPPLIER_ID)).thenReturn(true);
    }

    private void stubSummaryMetricsRepositoryDefaults() {
        when(salesOrdersRepository.countActiveCustomersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);
        when(salesOrdersRepository.countDeliveredOrdersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);
        when(salesOrdersRepository.sumSettledTonnesBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(salesOrdersRepository.findSupplierPortalProductBestSellingRankByCogs(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(null);
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
    void getSummaryMetrics_mapsTrendMetricsForProduct() throws Exception {
        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);

        when(salesOrdersRepository.countActiveCustomersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(8L, 4L);
        when(salesOrdersRepository.countDeliveredOrdersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(20L, 10L);
        when(salesOrdersRepository.sumSettledTonnesBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("12.5"), new BigDecimal("10"));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1000000"), new BigDecimal("800000"));
        when(salesOrdersRepository.findSupplierPortalProductBestSellingRankByCogs(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3);
        when(fileService.getImages(eq(PRODUCT_ID), eq(ReferenceType.PRODUCT))).thenReturn(null);

        ProductAnalysisSummaryMetricsV2DTO dto = service.getSummaryMetrics(
                PRODUCT_ID, null, null, start, end, principal);

        assertEquals(8L, dto.getActiveCustomers().getValue());
        assertEquals(100d, dto.getActiveCustomers().getTrendPercentage());
        assertEquals(20L, dto.getTotalOrders().getValue());
        assertEquals(0, new BigDecimal("12.5").compareTo(dto.getTotalQuantitySoldTonnes().getValue()));
        assertEquals(0, new BigDecimal("1000000").compareTo(dto.getTotalCogsTzs().getValue()));
        assertEquals(0, new BigDecimal("50000.00").compareTo(dto.getAverageOrderValueTzs().getValue()));
        assertEquals(-37.5d, dto.getAverageOrderValueTzs().getTrendPercentage());
        assertEquals("vs Previous Period", dto.getAverageOrderValueTzs().getTrendLabel());
        assertEquals(3, dto.getRanking());
    }

    @Test
    void getSummaryMetrics_aovZeroWhenNoDeliveredOrders() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        when(salesOrdersRepository.countActiveCustomersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);
        when(salesOrdersRepository.countDeliveredOrdersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);
        when(salesOrdersRepository.sumSettledTonnesBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(salesOrdersRepository.findSupplierPortalProductBestSellingRankByCogs(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(null);

        ProductAnalysisSummaryMetricsV2DTO dto = service.getSummaryMetrics(
                PRODUCT_ID, null, null, start, end, principal);

        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(dto.getAverageOrderValueTzs().getValue()));
        assertNull(dto.getAverageOrderValueTzs().getTrendPercentage());
        assertNull(dto.getRanking());
    }

    @Test
    void getSummaryMetrics_setsProductImageUrlWhenPresent() throws Exception {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        stubSummaryMetricsRepositoryDefaults();

        when(fileService.getImages(PRODUCT_ID, ReferenceType.PRODUCT))
                .thenReturn(new FileResponseDTO("https://cdn.example/product.jpg", "image/png", null, null, true));

        ProductAnalysisSummaryMetricsV2DTO dto = service.getSummaryMetrics(
                PRODUCT_ID, null, null, start, end, principal);

        assertEquals("https://cdn.example/product.jpg", dto.getProductImageUrl());
    }

    @Test
    void getSummaryMetrics_productImageUrlNullWhenMissing() throws Exception {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        stubSummaryMetricsRepositoryDefaults();
        when(fileService.getImages(PRODUCT_ID, ReferenceType.PRODUCT)).thenReturn(null);

        ProductAnalysisSummaryMetricsV2DTO dto = service.getSummaryMetrics(
                PRODUCT_ID, null, null, start, end, principal);

        assertNull(dto.getProductImageUrl());
    }

    @Test
    void getSummaryMetrics_rankingNullWhenNoQualifyingActivityInWindow() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        stubSummaryMetricsRepositoryDefaults();
        when(salesOrdersRepository.findSupplierPortalProductBestSellingRankByCogs(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(null);

        ProductAnalysisSummaryMetricsV2DTO dto = service.getSummaryMetrics(
                PRODUCT_ID, null, null, start, end, principal);

        assertNull(dto.getRanking());
    }

    @Test
    void getSummaryMetrics_trendPercentageTwoDecimalPrecision() {
        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);
        when(salesOrdersRepository.countActiveCustomersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L, 7L);
        when(salesOrdersRepository.countDeliveredOrdersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);
        when(salesOrdersRepository.sumSettledTonnesBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);

        ProductAnalysisSummaryMetricsV2DTO dto = service.getSummaryMetrics(
                PRODUCT_ID, null, null, start, end, principal);

        // (10 - 7) * 100 / 7 = 42.857... → rounds to 42.86 at scale 2
        assertEquals(42.86d, dto.getActiveCustomers().getTrendPercentage());
    }

    @Test
    void getSummaryMetrics_rankingQueryUsesCurrentSummaryWindow() {
        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 16);
        stubSummaryMetricsRepositoryDefaults();

        service.getSummaryMetrics(PRODUCT_ID, null, null, start, end, principal);

        org.mockito.ArgumentCaptor<LocalDateTime> fromCaptor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        org.mockito.ArgumentCaptor<LocalDateTime> toCaptor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(salesOrdersRepository).findSupplierPortalProductBestSellingRankByCogs(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                fromCaptor.capture(), toCaptor.capture());
        assertEquals(start.atStartOfDay(), fromCaptor.getValue());
        assertEquals(end.atTime(23, 59, 59, 999_999_999), toCaptor.getValue());
    }

    @Test
    void getSummaryMetrics_rejectsUnownedProduct() {
        when(productRepository.existsByPublicIdAndSupplierPublicId(PRODUCT_ID, SUPPLIER_ID)).thenReturn(false);

        assertThrows(BadRequestRestApiException.class, () -> service.getSummaryMetrics(
                PRODUCT_ID,
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                principal));

        verify(salesOrdersRepository, never()).countActiveCustomersBySupplierProductAndGeography(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getSalesTrend_zeroFillsDailySeriesAndHeadline() {
        Object[] dayTwo = {
                new BigDecimal("500"),
                Date.valueOf(LocalDate.of(2026, 3, 2)),
                9L,
                6L
        };

        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(dayTwo));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("500"), BigDecimal.ZERO);

        ProductAnalysisSalesTrendV2DTO dto = service.getSalesTrend(
                PRODUCT_ID, null, null, 3, 2026, principal);

        assertEquals(31, dto.getSeries().size());
        assertEquals(LocalDate.of(2026, 3, 1), dto.getSeries().get(0).getDate());
        assertEquals(LocalDate.of(2026, 3, 31), dto.getSeries().get(30).getDate());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getSeries().get(0).getTotalCogsTzs()));
        assertEquals(0L, dto.getSeries().get(0).getOrderCount());
        assertEquals(0L, dto.getSeries().get(0).getCustomerCount());
        assertEquals(0, new BigDecimal("500").compareTo(dto.getSeries().get(1).getTotalCogsTzs()));
        assertEquals(9L, dto.getSeries().get(1).getOrderCount());
        assertEquals(6L, dto.getSeries().get(1).getCustomerCount());
        assertEquals(0, new BigDecimal("500").compareTo(dto.getHeadline().getValue()));
        assertEquals(
                dto.getSeries().stream()
                        .map(ProductAnalysisDailyCogsPointDTO::getTotalCogsTzs)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                dto.getHeadline().getValue());
        assertEquals("vs Previous Period", dto.getHeadline().getTrendLabel());

        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationMonthForProduct(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getSalesTrend_resolvesMonthYearToFullCalendarWindow() {
        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);

        service.getSalesTrend(PRODUCT_ID, null, null, 2, 2026, principal);

        org.mockito.ArgumentCaptor<LocalDateTime> fromCaptor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        org.mockito.ArgumentCaptor<LocalDateTime> toCaptor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(salesOrdersRepository).aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                fromCaptor.capture(), toCaptor.capture());
        assertEquals(LocalDate.of(2026, 2, 1).atStartOfDay(), fromCaptor.getValue());
        assertEquals(LocalDate.of(2026, 2, 28).atTime(23, 59, 59, 999_999_999), toCaptor.getValue());
    }

    @Test
    void getDistrictRanking_noFilters_usesRegionRanking() {
        UUID regionA = UUID.randomUUID();
        Object[] first = {regionA, "Dar es Salaam", new BigDecimal("10")};

        when(salesOrdersRepository.listSupplierPortalProductRegionRankingBySettledTonnes(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(first));

        ProductAnalysisDistrictRankingV2DTO dto = service.getDistrictRanking(
                PRODUCT_ID, null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 1, principal);

        assertEquals(1, dto.getRows().size());
        assertEquals(regionA, dto.getRows().get(0).getOrganizationLocationPublicId());
        assertEquals("Dar es Salaam", dto.getRows().get(0).getOrganizationLocationName());
        verify(salesOrdersRepository, never()).listSupplierPortalProductWardRankingBySettledTonnes(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getDistrictRanking_regionFilter_usesWardRanking() {
        UUID wardA = UUID.randomUUID();
        UUID regionId = UUID.randomUUID();
        Object[] first = {wardA, "Kinondoni", new BigDecimal("10")};

        when(salesOrdersRepository.listSupplierPortalProductWardRankingBySettledTonnes(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(first));

        ProductAnalysisDistrictRankingV2DTO dto = service.getDistrictRanking(
                PRODUCT_ID, List.of(regionId), null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 1, principal);

        assertEquals("Kinondoni", dto.getRows().get(0).getOrganizationLocationName());
        verify(salesOrdersRepository, never()).listSupplierPortalProductRegionRankingBySettledTonnes(
                any(), any(), any(), any());
    }

    @Test
    void getDistrictRanking_locationFilter_usesWardRanking() {
        UUID locationId = UUID.randomUUID();
        UUID wardA = UUID.randomUUID();
        Object[] first = {wardA, "Kinondoni", new BigDecimal("10")};

        when(salesOrdersRepository.listSupplierPortalProductWardRankingBySettledTonnes(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(first));

        ProductAnalysisDistrictRankingV2DTO dto = service.getDistrictRanking(
                PRODUCT_ID, null, List.of(locationId), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 1, principal);

        assertEquals("Kinondoni", dto.getRows().get(0).getOrganizationLocationName());
        verify(salesOrdersRepository, never()).listSupplierPortalProductRegionRankingBySettledTonnes(
                any(), any(), any(), any());
    }

    @Test
    void getSummaryMetrics_rejectsNullProductId() {
        assertThrows(BadRequestRestApiException.class, () -> service.getSummaryMetrics(
                null,
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                principal));

        verify(salesOrdersRepository, never()).countActiveCustomersBySupplierProductAndGeography(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getSalesTrend_rejectsMonthWithoutYear() {
        assertThrows(BadRequestRestApiException.class, () -> service.getSalesTrend(
                PRODUCT_ID, null, null, 3, null, principal));
        assertThrows(BadRequestRestApiException.class, () -> service.getSalesTrend(
                PRODUCT_ID, null, null, 13, 2026, principal));

        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationMonthForProduct(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getSalesTrend_yearOnly_usesProductMonthlyAggregateAndTwelveZeroFilledBuckets() {
        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationMonthForProduct(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Arrays.asList(
                        new Object[]{new BigDecimal("100"), 2025, 1, 2L, 1L},
                        new Object[]{new BigDecimal("200"), 2025, 2, 3L, 2L}));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("250"));

        ProductAnalysisSalesTrendV2DTO dto = service.getSalesTrend(
                PRODUCT_ID, null, null, null, 2025, principal);

        assertEquals(12, dto.getSeries().size());
        assertEquals(LocalDate.of(2025, 1, 1), dto.getSeries().get(0).getDate());
        assertEquals(LocalDate.of(2025, 12, 1), dto.getSeries().get(11).getDate());
        assertEquals(0, new BigDecimal("100").compareTo(dto.getSeries().get(0).getTotalCogsTzs()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getSeries().get(2).getTotalCogsTzs()));
        assertEquals(0, new BigDecimal("300").compareTo(dto.getHeadline().getValue()));
        assertEquals(
                dto.getSeries().stream()
                        .map(ProductAnalysisDailyCogsPointDTO::getTotalCogsTzs)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                dto.getHeadline().getValue());
        assertEquals("vs Last Year", dto.getHeadline().getTrendLabel());

        verify(salesOrdersRepository).aggregateSupplierPortalLineRevenueByOrderCreationMonthForProduct(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class));
        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationMonthForAllSupplierProducts(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getDistrictRanking_rejectsInvalidLimit() {
        assertThrows(BadRequestRestApiException.class, () -> service.getDistrictRanking(
                PRODUCT_ID,
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                0,
                principal));

        verify(salesOrdersRepository, never()).listSupplierPortalProductWardRankingBySettledTonnes(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getDistrictRanking_rejectsInvalidDateRange() {
        assertThrows(BadRequestRestApiException.class, () -> service.getDistrictRanking(
                PRODUCT_ID,
                null,
                null,
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 9),
                10,
                principal));

        verify(salesOrdersRepository, never()).listSupplierPortalProductWardRankingBySettledTonnes(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getDistrictCogs_noFilters_usesRegionCogsAndLimitsRows() {
        UUID regionA = UUID.randomUUID();
        UUID regionB = UUID.randomUUID();
        Object[] first = {regionA, "Dar es Salaam", new BigDecimal("900000")};
        Object[] second = {regionB, "Arusha", new BigDecimal("500000")};

        when(salesOrdersRepository.listSupplierPortalProductRegionCogsByTotalCogs(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Arrays.asList(first, second));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1400000"));

        ProductAnalysisDistrictCogsV2DTO dto = service.getDistrictCogs(
                PRODUCT_ID, null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 1, principal);

        assertEquals(1, dto.getRows().size());
        assertEquals("Dar es Salaam", dto.getRows().get(0).getOrganizationLocationName());
        assertEquals(0, new BigDecimal("900000").compareTo(dto.getRows().get(0).getTotalCogsTzs()));
        assertEquals(0, new BigDecimal("1400000").compareTo(dto.getTotalCogsTzs()));
        verify(salesOrdersRepository, never()).listSupplierPortalProductWardCogsByTotalCogs(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getDistrictCogs_regionFilter_usesWardCogsUnchanged() {
        UUID regionId = UUID.randomUUID();
        UUID wardA = UUID.randomUUID();
        Object[] first = {wardA, "Kinondoni", new BigDecimal("900000")};

        when(salesOrdersRepository.listSupplierPortalProductWardCogsByTotalCogs(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.singletonList(first));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("900000"));

        ProductAnalysisDistrictCogsV2DTO dto = service.getDistrictCogs(
                PRODUCT_ID, List.of(regionId), null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 10, principal);

        assertEquals("Kinondoni", dto.getRows().get(0).getOrganizationLocationName());
        verify(salesOrdersRepository, never()).listSupplierPortalProductRegionCogsByTotalCogs(
                any(), any(), any(), any());
    }

    @Test
    void getWardPerformance_mapsPagedRows() {
        UUID wardA = UUID.randomUUID();
        Object[] row = {wardA, "Kinondoni", new BigDecimal("1000"), new BigDecimal("2.5"), 3L, 4L};
        org.springframework.data.domain.Page<Object[]> page =
                new org.springframework.data.domain.PageImpl<>(
                        Collections.singletonList(row),
                        org.springframework.data.domain.PageRequest.of(0, 20),
                        1);

        when(salesOrdersRepository.pageSupplierPortalProductWardPerformanceV2(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(salesOrdersRepository.listSupplierPortalProductWardPerformanceV2(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        org.springframework.data.domain.Page<ProductAnalysisWardPerformanceRowDTO> result = service.getWardPerformance(
                PRODUCT_ID,
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                principal,
                org.springframework.data.domain.PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        ProductAnalysisWardPerformanceRowDTO dto = result.getContent().get(0);
        assertEquals("Kinondoni", dto.getOrganizationLocationName());
        assertEquals(1, dto.getRank());
        assertEquals(3L, dto.getDistinctCustomerCount());
        assertEquals(4L, dto.getDeliveredOrderCount());
        assertEquals(0, new BigDecimal("250.00").compareTo(dto.getAverageOrderValueTzs()));
        assertNull(dto.getPeriodTrends().getTotalCogsTzsTrendPercentage());

        verify(salesOrdersRepository).listSupplierPortalProductWardPerformanceV2(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getWardPerformance_rejectsInvalidPageSize() {
        assertThrows(BadRequestRestApiException.class, () -> service.getWardPerformance(
                PRODUCT_ID,
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                principal,
                org.springframework.data.domain.PageRequest.of(0, 101)));

        verify(salesOrdersRepository, never()).pageSupplierPortalProductWardPerformanceV2(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any(), any());
    }

    @Test
    void getSalesCalendar_zeroFillsAllDaysInMonth() {
        Object[] dayTwo = {new BigDecimal("750"), Date.valueOf(LocalDate.of(2026, 2, 2))};

        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(dayTwo));
        when(salesOrdersRepository.countDeliveredOrdersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(4L);
        when(salesOrdersRepository.sumSettledTonnesBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("12.5"));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1500"));

        ProductAnalysisSalesCalendarV2DTO dto = service.getSalesCalendar(
                PRODUCT_ID, null, null, 2, 2026, principal);

        assertEquals(2, dto.getMonth());
        assertEquals(2026, dto.getYear());
        assertEquals(28, dto.getDays().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getDays().get(0).getTotalCogsTzs()));
        assertEquals(0, new BigDecimal("750").compareTo(dto.getDays().get(1).getTotalCogsTzs()));
        assertEquals(LocalDate.of(2026, 2, 28), dto.getDays().get(27).getDate());
        assertEquals(4L, dto.getTotalOrders());
        assertEquals(0, new BigDecimal("12.5").compareTo(dto.getTotalQuantitySoldTonnes()));
        assertEquals(0, new BigDecimal("1500").compareTo(dto.getTotalCogsTzs()));
        assertEquals(LocalDate.of(2026, 2, 2), dto.getPeakDay().getDate());
        assertEquals(0, new BigDecimal("750").compareTo(dto.getPeakDay().getTotalCogsTzs()));
        assertEquals("vs Previous Month", dto.getMonthlyOrdersVersusPreviousMonth().getTrendLabel());
        assertEquals("vs Previous Month", dto.getMonthlyUnitsSoldVersusPreviousMonth().getTrendLabel());
        assertEquals("vs Previous Month", dto.getPeakDay().getTotalCogsTzsVersusPreviousMonth().getTrendLabel());
    }

    @Test
    void getSalesCalendar_peakDayChoosesEarliestDayOnTie() {
        Object[] dayOne = {new BigDecimal("500"), Date.valueOf(LocalDate.of(2026, 3, 1))};
        Object[] dayThree = {new BigDecimal("500"), Date.valueOf(LocalDate.of(2026, 3, 3))};

        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(dayOne, dayThree));
        stubProductCalendarMonthlyTotals();

        ProductAnalysisSalesCalendarV2DTO dto = service.getSalesCalendar(
                PRODUCT_ID, null, null, 3, 2026, principal);

        assertEquals(LocalDate.of(2026, 3, 1), dto.getPeakDay().getDate());
    }

    @Test
    void getSalesCalendar_noSalesMonthReturnsNullPeakDayAndZeroSummaries() {
        when(salesOrdersRepository.aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(salesOrdersRepository.countDeliveredOrdersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(salesOrdersRepository.sumSettledTonnesBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        ProductAnalysisSalesCalendarV2DTO dto = service.getSalesCalendar(
                PRODUCT_ID, null, null, 2, 2026, principal);

        assertNull(dto.getPeakDay());
        assertEquals(0L, dto.getTotalOrders());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getTotalQuantitySoldTonnes()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getTotalCogsTzs()));
        assertEquals(28, dto.getDays().size());
    }

    private void stubProductCalendarMonthlyTotals() {
        when(salesOrdersRepository.countDeliveredOrdersBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2L);
        when(salesOrdersRepository.sumSettledTonnesBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("3"));
        when(salesOrdersRepository.sumSupplierPortalCogsBySupplierProductAndGeography(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(), anyBoolean(), any(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1000"));
    }

    @Test
    void getSalesCalendar_rejectsNullProductId() {
        assertThrows(BadRequestRestApiException.class, () -> service.getSalesCalendar(
                null, null, null, 3, 2026, principal));

        verify(salesOrdersRepository, never()).aggregateSupplierPortalLineRevenueByOrderCreationDayForProduct(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    @Test
    void getDistrictRanking_usesDefaultLimitWhenNull() {
        when(salesOrdersRepository.listSupplierPortalProductRegionRankingBySettledTonnes(
                eq(SUPPLIER_ID), eq(PRODUCT_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        ProductAnalysisDistrictRankingV2DTO dto = service.getDistrictRanking(
                PRODUCT_ID, null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null, principal);

        assertEquals(0, dto.getRows().size());
    }
}
