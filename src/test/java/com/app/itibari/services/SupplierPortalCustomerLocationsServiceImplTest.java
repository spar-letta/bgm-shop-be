package com.app.itibari.services;

import com.app.itibari.dataTypes.CustomerLocationMarkerCategoryV2;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.report.CustomerLocationDetailV2DTO;
import com.app.itibari.models.report.CustomerLocationsHeatMapV2DTO;
import com.app.itibari.models.report.CustomerLocationsMarkersV2DTO;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.SupplierPortalCustomerLocationSnapshotRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.services.supplierportal.SupplierPortalCustomerLocationsSupport;
import com.app.itibari.services.supplierportal.SupplierPortalCustomerLocationsValidation;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.exceptions.rest.ResourceNotFoundRestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.security.Principal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierPortalCustomerLocationsServiceImplTest {

    private static final UUID SUPPLIER_ID = UUID.fromString("30577548-e687-4115-9d51-3a33a363aa56");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String SUPPLIER_EMAIL = "supp@email.com";

    private SalesOrdersRepository salesOrdersRepository;
    private SupplierPortalCustomerLocationSnapshotRepository snapshotRepository;
    private PersonRepository personRepository;
    private SupplierRepository supplierRepository;
    private Principal principal;
    private SupplierPortalCustomerLocationsServiceImpl service;

    @BeforeEach
    void setUp() {
        salesOrdersRepository = mock(SalesOrdersRepository.class);
        snapshotRepository = mock(SupplierPortalCustomerLocationSnapshotRepository.class);
        personRepository = mock(PersonRepository.class);
        supplierRepository = mock(SupplierRepository.class);
        principal = mock(Principal.class);
        service = new SupplierPortalCustomerLocationsServiceImpl(
                snapshotRepository, salesOrdersRepository, personRepository, supplierRepository);
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
    void getMarkers_parsesNumericCoordinatesAndOmitsInvalidRows() {
        UUID regionId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        Timestamp recentOrder = Timestamp.valueOf(LocalDateTime.now().minusDays(5));

        Object[] valid = new Object[]{
                CUSTOMER_ID, "Shop Alpha", "-6.79", "39.20", "Active",
                recentOrder, regionId, "Dar es Salaam", wardId, "Kinondoni"
        };
        Object[] invalid = new Object[]{
                UUID.randomUUID(), "Shop Beta", "bad-lat", "39.20", "New",
                recentOrder, regionId, "Dar es Salaam", wardId, "Kinondoni"
        };
        when(snapshotRepository.listCustomerLocationMarkersFromSnapshot(
                eq(SUPPLIER_ID), eq(null), eq(null), eq(null),
                eq(SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1)))
                .thenReturn(List.<Object[]>of(valid, invalid));

        CustomerLocationsMarkersV2DTO response = service.getMarkers(null, null, null, principal);

        assertEquals(1, response.getMarkers().size());
        assertEquals(CUSTOMER_ID, response.getMarkers().get(0).getCustomerPublicId());
        double[] expectedCenter = SupplierPortalCustomerLocationsSupport.toGeohashCellCenter(
                -6.79d, 39.20d, SupplierPortalCustomerLocationsValidation.HEAT_MAP_GEOHASH_PRECISION);
        assertEquals(expectedCenter[0], response.getMarkers().get(0).getLatitude(), 0.000001d);
        assertEquals(expectedCenter[1], response.getMarkers().get(0).getLongitude(), 0.000001d);
        assertTrue(Math.abs(response.getMarkers().get(0).getLatitude() - (-6.79d)) > 0.00001d
                || Math.abs(response.getMarkers().get(0).getLongitude() - 39.20d) > 0.00001d);
        assertEquals(CustomerLocationMarkerCategoryV2.ACTIVE, response.getMarkers().get(0).getCategory());
        assertEquals(regionId, response.getMarkers().get(0).getRegionPublicId());
        assertEquals("Dar es Salaam", response.getMarkers().get(0).getRegionName());
        assertEquals(wardId, response.getMarkers().get(0).getOrganizationLocationPublicId());
        assertEquals("Kinondoni", response.getMarkers().get(0).getOrganizationLocationName());
        assertEquals(1, response.getMeta().getReturnedCount());
        assertEquals(1, response.getMeta().getOmittedInvalidCoordinates());
        assertEquals(5000, response.getMeta().getMaxMarkers());
        assertFalse(response.getMeta().isTruncated());
    }

    @Test
    void getMarkers_truncatesAtMaxMarkers() {
        Timestamp recentOrder = Timestamp.valueOf(LocalDateTime.now().minusDays(5));

        java.util.ArrayList<Object[]> rows = new java.util.ArrayList<>();
        for (int i = 0; i < SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1; i++) {
            rows.add(new Object[]{
                    UUID.randomUUID(), "Shop " + i, "-6.79", "39.20", "Active",
                    recentOrder, UUID.randomUUID(), "Region", UUID.randomUUID(), "Ward"
            });
        }
        when(snapshotRepository.listCustomerLocationMarkersFromSnapshot(
                eq(SUPPLIER_ID), eq(null), eq(null), eq(null),
                eq(SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1)))
                .thenReturn(rows);

        CustomerLocationsMarkersV2DTO response = service.getMarkers(null, null, null, principal);

        assertEquals(SupplierPortalCustomerLocationsValidation.MAX_MARKERS, response.getMarkers().size());
        assertTrue(response.getMeta().isTruncated());
        assertEquals(SupplierPortalCustomerLocationsValidation.MAX_MARKERS, response.getMeta().getReturnedCount());
    }

    @Test
    void getMarkers_overridesToChurnedWhenLastOrderOlderThanThirtyDays() {
        Object[] row = new Object[]{
                CUSTOMER_ID, "Shop Alpha", "-6.79", "39.20", "Active",
                Timestamp.valueOf(LocalDateTime.now().minusDays(31)),
                UUID.randomUUID(), "Region", UUID.randomUUID(), "Ward"
        };
        when(snapshotRepository.listCustomerLocationMarkersFromSnapshot(
                eq(SUPPLIER_ID), eq(null), eq(null), eq(null),
                eq(SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1)))
                .thenReturn(List.<Object[]>of(row));

        CustomerLocationsMarkersV2DTO response = service.getMarkers(null, null, null, principal);

        assertEquals(1, response.getMarkers().size());
        assertEquals(CustomerLocationMarkerCategoryV2.CHURNED, response.getMarkers().get(0).getCategory());
    }

    @Test
    void getCustomerDetail_returnsBaseWithoutPeriodMetricsWhenDatesOmitted() {
        Object[] baseRow = new Object[]{
                CUSTOMER_ID, "Store One", "2557123456789", "-6.79", "39.20", "Inactive",
                "Near market", "123 Street",
                UUID.randomUUID(), "Dar", UUID.randomUUID(), "Kinondoni"
        };
        when(salesOrdersRepository.findCustomerLocationDetailBaseV2(
                eq(SUPPLIER_ID), eq(CUSTOMER_ID), eq(null), eq(null), eq(null)))
                .thenReturn(List.<Object[]>of(baseRow));

        CustomerLocationDetailV2DTO detail = service.getCustomerDetail(
                CUSTOMER_ID, null, null, null, null, null, principal);

        assertEquals(CUSTOMER_ID, detail.getCustomerPublicId());
        assertEquals("Store One", detail.getCustomerName());
        assertTrue(detail.isPhoneVisible());
        assertEquals("*** *** 789", detail.getPhoneNumber());
        assertEquals(CustomerLocationMarkerCategoryV2.INACTIVE, detail.getCategory());
        assertNull(detail.getPeriodMetrics());
        verify(salesOrdersRepository, never()).aggregateCustomerLocationPeriodMetricsV2(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCustomerDetail_includesPeriodMetricsWhenBothDatesProvided() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        Object[] baseRow = new Object[]{
                CUSTOMER_ID, "Store One", "0712345678", "-6.79", "39.20", "New",
                null, null,
                UUID.randomUUID(), "Dar", UUID.randomUUID(), "Kinondoni"
        };
        Object[] metricsRow = new Object[]{
                3L, BigDecimal.valueOf(150000), BigDecimal.valueOf(2.5),
                Timestamp.valueOf(LocalDateTime.of(2026, 3, 15, 10, 0))
        };

        when(salesOrdersRepository.findCustomerLocationDetailBaseV2(
                eq(SUPPLIER_ID), eq(CUSTOMER_ID), eq(null), eq(null), eq(null)))
                .thenReturn(List.<Object[]>of(baseRow));
        when(salesOrdersRepository.aggregateCustomerLocationPeriodMetricsV2(
                eq(SUPPLIER_ID), eq(CUSTOMER_ID), eq(null), eq(null), eq(null), any(), any()))
                .thenReturn(List.<Object[]>of(metricsRow));

        CustomerLocationDetailV2DTO detail = service.getCustomerDetail(
                CUSTOMER_ID, null, null, null, start, end, principal);

        assertNotNull(detail.getPeriodMetrics());
        assertEquals(start, detail.getPeriodMetrics().getStartDate());
        assertEquals(end, detail.getPeriodMetrics().getEndDate());
        assertEquals(3L, detail.getPeriodMetrics().getDeliveredOrderCount());
        assertEquals(0, BigDecimal.valueOf(150000).compareTo(detail.getPeriodMetrics().getTotalCogsTzs()));
    }

    @Test
    void getCustomerDetail_setsPhoneVisibleFalseWhenNoPhoneOnFile() {
        Object[] baseRow = new Object[]{
                CUSTOMER_ID, "Store One", null, "-6.79", "39.20", "New",
                null, null,
                UUID.randomUUID(), "Dar", UUID.randomUUID(), "Kinondoni"
        };
        when(salesOrdersRepository.findCustomerLocationDetailBaseV2(
                eq(SUPPLIER_ID), eq(CUSTOMER_ID), eq(null), eq(null), eq(null)))
                .thenReturn(List.<Object[]>of(baseRow));

        CustomerLocationDetailV2DTO detail = service.getCustomerDetail(
                CUSTOMER_ID, null, null, null, null, null, principal);

        assertFalse(detail.isPhoneVisible());
        assertNull(detail.getPhoneNumber());
    }

    @Test
    void getCustomerDetail_rejectsOneSidedDates() {
        assertThrows(BadRequestRestApiException.class, () ->
                service.getCustomerDetail(CUSTOMER_ID, null, null, null,
                        LocalDate.of(2026, 3, 1), null, principal));
    }

    @Test
    void getCustomerDetail_throws404WhenOutOfScope() {
        when(salesOrdersRepository.findCustomerLocationDetailBaseV2(
                eq(SUPPLIER_ID), eq(CUSTOMER_ID), eq(null), eq(null), eq(null)))
                .thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundRestApiException.class, () ->
                service.getCustomerDetail(CUSTOMER_ID, null, null, null, null, null, principal));
    }

    @Test
    void getHeatMap_groupsAllActiveCustomers() {
        Object[] row = new Object[]{
                CUSTOMER_ID, "-6.79", "39.20", "Active",
                Timestamp.valueOf(LocalDateTime.now().minusDays(5))
        };
        when(snapshotRepository.listCustomerLocationHeatMapPointsFromSnapshot(
                eq(SUPPLIER_ID), eq(null), eq(null), eq(null),
                eq(SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1)))
                .thenReturn(List.<Object[]>of(row));

        CustomerLocationsHeatMapV2DTO response = service.getHeatMap(null, null, null, null, principal);

        assertEquals(1, response.getPoints().get(CustomerLocationMarkerCategoryV2.ACTIVE).size());
        assertEquals(1, response.getPoints().get(CustomerLocationMarkerCategoryV2.ACTIVE).get(0).getCount());
        assertEquals(0, response.getPoints().get(CustomerLocationMarkerCategoryV2.CHURNED).size());
        assertEquals(1L, response.getMeta().getCountByCategory().get(CustomerLocationMarkerCategoryV2.ACTIVE));
        assertEquals(3, response.getPoints().size());
        assertFalse(response.getPoints().containsKey(CustomerLocationMarkerCategoryV2.INACTIVE));
    }

    @Test
    void getHeatMap_assignsMixedCategories() {
        Object[] active = new Object[]{
                CUSTOMER_ID, "-6.79", "39.20", "Active",
                Timestamp.valueOf(LocalDateTime.now().minusDays(5))
        };
        Object[] inactive = new Object[]{
                UUID.randomUUID(), "-6.80", "39.21", "Inactive",
                Timestamp.valueOf(LocalDateTime.now().minusDays(10))
        };
        Object[] newCustomer = new Object[]{
                UUID.randomUUID(), "-6.81", "39.22", "New",
                Timestamp.valueOf(LocalDateTime.now().minusDays(1))
        };
        when(snapshotRepository.listCustomerLocationHeatMapPointsFromSnapshot(
                eq(SUPPLIER_ID), eq(null), eq(null), eq(null),
                eq(SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1)))
                .thenReturn(List.<Object[]>of(active, inactive, newCustomer));

        CustomerLocationsHeatMapV2DTO response = service.getHeatMap(null, null, null, null, principal);

        assertEquals(1, response.getPoints().get(CustomerLocationMarkerCategoryV2.ACTIVE).size());
        assertFalse(response.getPoints().containsKey(CustomerLocationMarkerCategoryV2.INACTIVE));
        assertEquals(1, response.getPoints().get(CustomerLocationMarkerCategoryV2.NEW).size());
        assertEquals(2, response.getMeta().getReturnedCount());
        assertEquals(2L, response.getMeta().getCountByCategory().get(CustomerLocationMarkerCategoryV2.ACTIVE)
                + response.getMeta().getCountByCategory().get(CustomerLocationMarkerCategoryV2.NEW));
        assertNull(response.getMeta().getCountByCategory().get(CustomerLocationMarkerCategoryV2.INACTIVE));
    }

    @Test
    void getHeatMap_mergesCustomersInSameGeohashCell() {
        Timestamp recent = Timestamp.valueOf(LocalDateTime.now().minusDays(5));
        // Same street-level area → same precision-5 geohash cell
        Object[] a = new Object[]{CUSTOMER_ID, "-6.7920", "39.2080", "Active", recent};
        Object[] b = new Object[]{UUID.randomUUID(), "-6.7925", "39.2085", "Active", recent};
        when(snapshotRepository.listCustomerLocationHeatMapPointsFromSnapshot(
                eq(SUPPLIER_ID), eq(null), eq(null), eq(null),
                eq(SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1)))
                .thenReturn(List.<Object[]>of(a, b));

        CustomerLocationsHeatMapV2DTO response = service.getHeatMap(null, null, null, null, principal);

        assertEquals(1, response.getPoints().get(CustomerLocationMarkerCategoryV2.ACTIVE).size());
        assertEquals(2, response.getPoints().get(CustomerLocationMarkerCategoryV2.ACTIVE).get(0).getCount());
        assertEquals(1, response.getMeta().getReturnedCount());
        assertEquals(2L, response.getMeta().getCountByCategory().get(CustomerLocationMarkerCategoryV2.ACTIVE));
        // Cell center must not equal either exact input coordinate
        double cellLat = response.getPoints().get(CustomerLocationMarkerCategoryV2.ACTIVE).get(0).getLat();
        double cellLng = response.getPoints().get(CustomerLocationMarkerCategoryV2.ACTIVE).get(0).getLng();
        assertTrue(Math.abs(cellLat - (-6.7920d)) > 0.00001d || Math.abs(cellLng - 39.2080d) > 0.00001d);
    }

    @Test
    void getHeatMap_overridesToChurnedWhenLastOrderOlderThanThirtyDays() {
        Object[] row = new Object[]{
                CUSTOMER_ID, "-6.79", "39.20", "Active",
                Timestamp.valueOf(LocalDateTime.now().minusDays(45))
        };
        when(snapshotRepository.listCustomerLocationHeatMapPointsFromSnapshot(
                eq(SUPPLIER_ID), eq(null), eq(null), eq(null),
                eq(SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1)))
                .thenReturn(List.<Object[]>of(row));

        CustomerLocationsHeatMapV2DTO response = service.getHeatMap(null, null, null, null, principal);

        assertEquals(1, response.getPoints().get(CustomerLocationMarkerCategoryV2.CHURNED).size());
        assertEquals(0, response.getPoints().get(CustomerLocationMarkerCategoryV2.ACTIVE).size());
    }

    @Test
    void getHeatMap_treatsNullMaxOrderTimeAsChurned() {
        Object[] row = new Object[]{
                CUSTOMER_ID, "-6.79", "39.20", "Active", null
        };
        when(snapshotRepository.listCustomerLocationHeatMapPointsFromSnapshot(
                eq(SUPPLIER_ID), eq(null), eq(null), eq(null),
                eq(SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1)))
                .thenReturn(List.<Object[]>of(row));

        CustomerLocationsHeatMapV2DTO response = service.getHeatMap(null, null, null, null, principal);

        assertEquals(1, response.getPoints().get(CustomerLocationMarkerCategoryV2.CHURNED).size());
    }

    @Test
    void getHeatMap_skipsInvalidCoordinates() {
        Object[] invalid = new Object[]{
                CUSTOMER_ID, "bad-lat", "39.20", "Active",
                Timestamp.valueOf(LocalDateTime.now().minusDays(5))
        };
        when(snapshotRepository.listCustomerLocationHeatMapPointsFromSnapshot(
                eq(SUPPLIER_ID), eq(null), eq(null), eq(null),
                eq(SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1)))
                .thenReturn(List.<Object[]>of(invalid));

        CustomerLocationsHeatMapV2DTO response = service.getHeatMap(null, null, null, null, principal);

        assertEquals(0, response.getMeta().getReturnedCount());
        assertTrue(response.getPoints().values().stream().allMatch(List::isEmpty));
    }

    @Test
    void getHeatMap_truncatesWhenSqlReturnsSentinelRow() {
        Timestamp recentOrder = Timestamp.valueOf(LocalDateTime.now().minusDays(5));

        java.util.ArrayList<Object[]> rows = new java.util.ArrayList<>();
        for (int i = 0; i < SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1; i++) {
            rows.add(new Object[]{
                    UUID.randomUUID(), "-6.79", "39.20", "Active", recentOrder
            });
        }
        when(snapshotRepository.listCustomerLocationHeatMapPointsFromSnapshot(
                eq(SUPPLIER_ID), eq(null), eq(null), eq(null),
                eq(SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1)))
                .thenReturn(rows);

        CustomerLocationsHeatMapV2DTO response = service.getHeatMap(null, null, null, null, principal);

        assertTrue(response.getMeta().isTruncated());
        // All same coords → one geohash cell with MAX_MARKERS customers
        assertEquals(1, response.getMeta().getReturnedCount());
        assertEquals(1, response.getPoints().get(CustomerLocationMarkerCategoryV2.ACTIVE).size());
        assertEquals(
                SupplierPortalCustomerLocationsValidation.MAX_MARKERS,
                response.getPoints().get(CustomerLocationMarkerCategoryV2.ACTIVE).get(0).getCount());
        assertEquals(
                (long) SupplierPortalCustomerLocationsValidation.MAX_MARKERS,
                response.getMeta().getCountByCategory().get(CustomerLocationMarkerCategoryV2.ACTIVE));
    }

    @Test
    void getHeatMap_filtersToRequestedCategoryOnly() {
        Object[] active = new Object[]{
                CUSTOMER_ID, "-6.79", "39.20", "Active",
                Timestamp.valueOf(LocalDateTime.now().minusDays(5))
        };
        Object[] newCustomer = new Object[]{
                UUID.randomUUID(), "-6.81", "39.22", "New",
                Timestamp.valueOf(LocalDateTime.now().minusDays(1))
        };
        Object[] churned = new Object[]{
                UUID.randomUUID(), "-6.82", "39.23", "Active",
                Timestamp.valueOf(LocalDateTime.now().minusDays(45))
        };
        when(snapshotRepository.listCustomerLocationHeatMapPointsFromSnapshot(
                eq(SUPPLIER_ID), eq(null), eq(null), eq(null),
                eq(SupplierPortalCustomerLocationsValidation.MAX_MARKERS + 1)))
                .thenReturn(List.<Object[]>of(active, newCustomer, churned));

        CustomerLocationsHeatMapV2DTO response = service.getHeatMap(
                null, null, null, CustomerLocationMarkerCategoryV2.ACTIVE, principal);

        assertEquals(1, response.getPoints().get(CustomerLocationMarkerCategoryV2.ACTIVE).size());
        assertEquals(1, response.getPoints().get(CustomerLocationMarkerCategoryV2.ACTIVE).get(0).getCount());
        assertTrue(response.getPoints().get(CustomerLocationMarkerCategoryV2.NEW).isEmpty());
        assertTrue(response.getPoints().get(CustomerLocationMarkerCategoryV2.CHURNED).isEmpty());
        assertEquals(1L, response.getMeta().getCountByCategory().get(CustomerLocationMarkerCategoryV2.ACTIVE));
        assertEquals(0L, response.getMeta().getCountByCategory().get(CustomerLocationMarkerCategoryV2.NEW));
        assertEquals(0L, response.getMeta().getCountByCategory().get(CustomerLocationMarkerCategoryV2.CHURNED));
        assertEquals(1, response.getMeta().getReturnedCount());
    }

    @Test
    void getHeatMap_rejectsInactiveCategoryFilter() {
        assertThrows(BadRequestRestApiException.class, () ->
                service.getHeatMap(null, null, null, CustomerLocationMarkerCategoryV2.INACTIVE, principal));
        verify(snapshotRepository, never()).listCustomerLocationHeatMapPointsFromSnapshot(
                any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }
}
