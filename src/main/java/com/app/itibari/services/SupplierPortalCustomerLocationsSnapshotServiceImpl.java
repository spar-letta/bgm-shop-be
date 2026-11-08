package com.app.itibari.services;

import com.app.itibari.repository.SupplierPortalCustomerLocationSnapshotRepository;
import com.app.itibari.services.supplierportal.SupplierPortalCustomerLocationSnapshotRefreshException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierPortalCustomerLocationsSnapshotServiceImpl
        implements SupplierPortalCustomerLocationsSnapshotService {

    private final SupplierPortalCustomerLocationSnapshotRepository snapshotRepository;

    @Override
    @Transactional
    public void refreshSnapshot() {
        LocalDate snapshotAsOf = LocalDate.now().minusDays(1);
        LocalDateTime cutoff = LocalDate.now().atStartOfDay();
        long existingRows = snapshotRepository.countSnapshotRows();

        log.info(
                "Supplier portal customer location snapshot refresh started (asOf={}, cutoff={}, existingRows={})",
                snapshotAsOf,
                cutoff,
                existingRows);

        snapshotRepository.deleteAllSnapshotRows();
        int inserted = insertSnapshotRowsOrAbort(snapshotAsOf, cutoff, existingRows);

        log.info("Supplier portal customer location snapshot refresh finished: rows={}", inserted);
    }

    private int insertSnapshotRowsOrAbort(LocalDate snapshotAsOf,
                                          LocalDateTime cutoff,
                                          long existingRows) {
        try {
            int inserted = snapshotRepository.insertSnapshotRows(snapshotAsOf, cutoff);
            if (inserted == 0 && existingRows > 0) {
                throw new SupplierPortalCustomerLocationSnapshotRefreshException(
                        "Refusing to replace customer location snapshot with 0 rows when "
                                + existingRows + " rows existed");
            }
            return inserted;
        } catch (SupplierPortalCustomerLocationSnapshotRefreshException ex) {
            log.error(
                    "Supplier portal customer location snapshot refresh aborted; prior snapshot will be retained",
                    ex);
            throw ex;
        } catch (RuntimeException ex) {
            log.error(
                    "Supplier portal customer location snapshot insert failed; prior snapshot will be retained",
                    ex);
            throw new SupplierPortalCustomerLocationSnapshotRefreshException(
                    "Customer location snapshot insert failed", ex);
        }
    }
}
