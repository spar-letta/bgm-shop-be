package com.app.itibari.services.supplierportal;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SupplierPortalDashboardMathTest {

    @Test
    public void inclusivePreviousWindow_singleDay() {
        LocalDate[] window = SupplierPortalDashboardMath.inclusivePreviousWindow(
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 10));
        assertArrayEquals(new LocalDate[]{LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 9)}, window);
    }

    @Test
    public void inclusivePreviousWindow_week() {
        LocalDate[] window = SupplierPortalDashboardMath.inclusivePreviousWindow(
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 16));
        assertArrayEquals(new LocalDate[]{LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 9)}, window);
    }

    @Test
    public void inclusivePreviousWindow_fullCalendarMay_isRolling31DaysNotAprilMonth() {
        LocalDate[] window = SupplierPortalDashboardMath.inclusivePreviousWindow(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31));
        // 31 inclusive days immediately before May 1st → Mar 31 through Apr 30 (not calendar April 1–30).
        assertArrayEquals(new LocalDate[]{LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30)}, window);
    }

    @Test
    public void inclusivePreviousWindow_rolling30Days() {
        LocalDate[] window = SupplierPortalDashboardMath.inclusivePreviousWindow(
                LocalDate.of(2026, 3, 3),
                LocalDate.of(2026, 4, 1));
        assertArrayEquals(new LocalDate[]{LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 2)}, window);
    }

    @Test
    public void inclusivePreviousWindow_90Days() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = start.plusDays(89);
        LocalDate[] window = SupplierPortalDashboardMath.inclusivePreviousWindow(start, end);
        assertEquals(90, java.time.temporal.ChronoUnit.DAYS.between(window[0], window[1]) + 1);
        assertEquals(window[1].plusDays(1), start);
    }

    @Test
    public void percentChange_handlesZeroBaseline() {
        assertNull(SupplierPortalDashboardMath.percentChange(BigDecimal.TEN, BigDecimal.ZERO));
    }

    @Test
    public void percentChange_handlesNullBaseline() {
        assertNull(SupplierPortalDashboardMath.percentChange(BigDecimal.TEN, null));
    }

    @Test
    public void percentChange_math() {
        assertEquals(25d, SupplierPortalDashboardMath.percentChange(
                BigDecimal.valueOf(125), BigDecimal.valueOf(100)), 0.0001d);
    }

    @Test
    public void percentChange_twoDecimalPrecision() {
        // (100 - 75) * 100 / 75 = 33.3333... → rounds to 33.33 at scale 2
        assertEquals(33.33d, SupplierPortalDashboardMath.percentChange(
                BigDecimal.valueOf(100), BigDecimal.valueOf(75)), 0.0001d);
    }

    @Test
    public void intensityRatio_andLevel() {
        assertEquals(0.5d, SupplierPortalDashboardMath.intensityRatio(
                BigDecimal.valueOf(50), BigDecimal.valueOf(100)), 0.0001d);
        assertEquals("NONE", SupplierPortalDashboardMath.intensityLevel(0.0d));
        assertEquals("LOW", SupplierPortalDashboardMath.intensityLevel(0.1d));
        assertEquals("PEAK", SupplierPortalDashboardMath.intensityLevel(1.0d));
    }

    @Test
    public void percentChangeOrZero_defaultsWhenBaselineZeroOrMissing() {
        assertEquals(0.0d, SupplierPortalDashboardMath.percentChangeOrZero(BigDecimal.TEN, BigDecimal.ZERO), 0.0001d);
        assertEquals(0.0d, SupplierPortalDashboardMath.percentChangeOrZero(BigDecimal.TEN, null), 0.0001d);
        assertEquals(0.0d, SupplierPortalDashboardMath.percentChangeOrZero(BigDecimal.ZERO, BigDecimal.ZERO), 0.0001d);
    }

    @Test
    public void percentChangeOrZero_preservesNormalMath() {
        assertEquals(25d, SupplierPortalDashboardMath.percentChangeOrZero(
                BigDecimal.valueOf(125), BigDecimal.valueOf(100)), 0.0001d);
    }

    @Test
    public void formatLabels() {
        assertEquals("TZS 1,250,000", SupplierPortalDashboardMath.formatTzsLabel(new BigDecimal("1250000")));
        assertEquals("12.5 Tonnes", SupplierPortalDashboardMath.formatTonnesLabel(new BigDecimal("12.50")));
    }
}
