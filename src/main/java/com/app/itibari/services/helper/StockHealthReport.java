package com.app.itibari.services.helper;

import java.time.LocalDate;
import java.util.List;

/**
 * Result of the nightly read-only stock health pass: layer drift plus
 * low-stock (reorder-point) findings. Consumed by the log report and the
 * Google Chat notifier.
 */
public record StockHealthReport(
        LocalDate date,
        int checked,
        int drifted,
        int missingSnapshot,
        int missingLive,
        int lowStockCount,
        List<DriftEntry> topDrifts,
        List<LowStockEntry> lowStock) {

    public record DriftEntry(String productName, String sku, String warehouse,
                             long operational, Long snapshot, Integer liveClosing) {

        /** Worst absolute disagreement against the operational layer, used to rank offenders. */
        public long magnitude() {
            long snapshotDelta = snapshot == null ? 0 : Math.abs(operational - snapshot);
            long liveDelta = liveClosing == null ? 0 : Math.abs(operational - liveClosing);
            return Math.max(snapshotDelta, liveDelta);
        }
    }

    public record LowStockEntry(String productName, String sku, String warehouse,
                                long available, String suppliers) {
    }
}
