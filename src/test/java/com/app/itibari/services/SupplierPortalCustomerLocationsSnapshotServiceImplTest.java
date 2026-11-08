package com.app.itibari.services;

import com.app.itibari.repository.SupplierPortalCustomerLocationSnapshotRepository;
import com.app.itibari.services.supplierportal.SupplierPortalCustomerLocationSnapshotRefreshException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierPortalCustomerLocationsSnapshotServiceImplTest {

    private SupplierPortalCustomerLocationSnapshotRepository snapshotRepository;
    private SupplierPortalCustomerLocationsSnapshotServiceImpl service;

    @BeforeEach
    void setUp() {
        snapshotRepository = mock(SupplierPortalCustomerLocationSnapshotRepository.class);
        service = new SupplierPortalCustomerLocationsSnapshotServiceImpl(snapshotRepository);
    }

    @Test
    void refreshSnapshot_replacesRowsThroughStartOfToday() {
        when(snapshotRepository.countSnapshotRows()).thenReturn(10L);
        when(snapshotRepository.insertSnapshotRows(
                eq(LocalDate.now().minusDays(1)),
                eq(LocalDate.now().atStartOfDay())))
                .thenReturn(42);

        service.refreshSnapshot();

        var order = inOrder(snapshotRepository);
        order.verify(snapshotRepository).countSnapshotRows();
        order.verify(snapshotRepository).deleteAllSnapshotRows();
        order.verify(snapshotRepository).insertSnapshotRows(
                eq(LocalDate.now().minusDays(1)),
                eq(LocalDate.now().atStartOfDay()));
    }

    @Test
    void refreshSnapshot_allowsEmptySnapshotOnFirstLoad() {
        when(snapshotRepository.countSnapshotRows()).thenReturn(0L);
        when(snapshotRepository.insertSnapshotRows(
                eq(LocalDate.now().minusDays(1)),
                eq(LocalDate.now().atStartOfDay())))
                .thenReturn(0);

        service.refreshSnapshot();

        verify(snapshotRepository).deleteAllSnapshotRows();
    }

    @Test
    void refreshSnapshot_abortsWhenInsertWouldWipeExistingData() {
        when(snapshotRepository.countSnapshotRows()).thenReturn(100L);
        when(snapshotRepository.insertSnapshotRows(
                eq(LocalDate.now().minusDays(1)),
                eq(LocalDate.now().atStartOfDay())))
                .thenReturn(0);

        SupplierPortalCustomerLocationSnapshotRefreshException ex = assertThrows(
                SupplierPortalCustomerLocationSnapshotRefreshException.class,
                () -> service.refreshSnapshot());

        assertEquals(
                "Refusing to replace customer location snapshot with 0 rows when 100 rows existed",
                ex.getMessage());
    }

    @Test
    void refreshSnapshot_wrapsInsertFailures() {
        when(snapshotRepository.countSnapshotRows()).thenReturn(50L);
        when(snapshotRepository.insertSnapshotRows(
                eq(LocalDate.now().minusDays(1)),
                eq(LocalDate.now().atStartOfDay())))
                .thenThrow(new RuntimeException("db timeout"));

        SupplierPortalCustomerLocationSnapshotRefreshException ex = assertThrows(
                SupplierPortalCustomerLocationSnapshotRefreshException.class,
                () -> service.refreshSnapshot());

        assertEquals("Customer location snapshot insert failed", ex.getMessage());
        assertEquals("db timeout", ex.getCause().getMessage());
    }
}
