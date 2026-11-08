package com.app.itibari.models.report;

import java.math.BigDecimal;

/**
 * Single-pass aggregate for Supplier Portal V2 dashboard summary-metrics.
 * <p>For the demo-safe perf fix, customer/order counts match today's summary-metrics output:
 * {@code COUNT(c)} / {@code COUNT(so)} over supplier-attributed order lines (join-expanded,
 * not {@code COUNT(DISTINCT …)}). Elsewhere in Supplier Portal V2, DISTINCT is the norm;
 * aligning summary-metrics with that intended semantics is a separate follow-up after product sign-off.</p>
 */
public record SupplierPortalDashboardSummaryMetricsRow(
        long activeCustomers,
        long totalOrders,
        BigDecimal totalQuantitySoldTonnes,
        BigDecimal totalCogsTzs) {

    public static SupplierPortalDashboardSummaryMetricsRow zero() {
        return new SupplierPortalDashboardSummaryMetricsRow(0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * Maps a native-query row. Hibernate may return a single-element array wrapping the column tuple.
     */
    public static SupplierPortalDashboardSummaryMetricsRow fromAggregateRow(Object[] row) {
        Object[] columns = unwrapAggregateColumns(row);
        if (columns.length < 4) {
            return zero();
        }
        return new SupplierPortalDashboardSummaryMetricsRow(
                toLong(columns[0]),
                toLong(columns[1]),
                toBigDecimal(columns[2]),
                toBigDecimal(columns[3]));
    }

    private static Object[] unwrapAggregateColumns(Object[] row) {
        if (row == null) {
            return new Object[0];
        }
        if (row.length == 1 && row[0] instanceof Object[] nested) {
            return nested;
        }
        return row;
    }

    private static long toLong(Object raw) {
        if (raw == null) {
            return 0L;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Expected numeric aggregate, got " + raw.getClass());
    }

    private static BigDecimal toBigDecimal(Object raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        if (raw instanceof BigDecimal bd) {
            return bd;
        }
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        throw new IllegalArgumentException("Expected numeric aggregate, got " + raw.getClass());
    }
}
