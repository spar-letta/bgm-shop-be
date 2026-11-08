package com.app.itibari.services.supplierportal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pure helpers for supplier portal dashboard windows and trend math — kept intentionally small/testable.
 */
public final class SupplierPortalDashboardMath {

    private SupplierPortalDashboardMath() {
    }

    /**
     * Rolling equal-length previous period: count inclusive days in the selected range, then take the
     * immediately preceding block of that length ending the day before {@code currentStart}.
     *
     * <p>Example: current [10th, 16th] inclusive → seven days → previous [3rd, 9th] inclusive.</p>
     * <p>Not calendar-month aware: full May (May 1–31, 31 days) compares to Mar 31–Apr 30, not April 1–30.
     * UI presets labelled “Last Month” should explain this rule or use dates that match product intent.</p>
     *
     * @return {@code previousStart} (inclusive) and {@code previousEnd} (inclusive)
     */
    public static LocalDate[] inclusivePreviousWindow(LocalDate currentStartInclusive, LocalDate currentEndInclusive) {
        long inclusiveDays = ChronoUnit.DAYS.between(currentStartInclusive, currentEndInclusive) + 1;
        LocalDate previousEndInclusive = currentStartInclusive.minusDays(1);
        LocalDate previousStartInclusive = currentStartInclusive.minusDays(inclusiveDays);
        return new LocalDate[]{previousStartInclusive, previousEndInclusive};
    }

    public static Double percentChange(BigDecimal current, BigDecimal baseline) {
        if (baseline == null || baseline.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        if (current == null) {
            current = BigDecimal.ZERO;
        }
        return current.subtract(baseline)
                .multiply(BigDecimal.valueOf(100))
                .divide(baseline, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static Double percentChangeLong(long current, long baseline) {
        return percentChange(BigDecimal.valueOf(current), BigDecimal.valueOf(baseline));
    }

    public static Double percentChangeLong(Long current, Long baseline) {
        long c = current != null ? current : 0L;
        long b = baseline != null ? baseline : 0L;
        return percentChange(BigDecimal.valueOf(c), BigDecimal.valueOf(b));
    }

    /**
     * V2 dashboard trend envelopes: same formula as {@link #percentChange}, but returns {@code 0.0} when the baseline
     * is null or zero instead of {@code null}.
     */
    public static Double percentChangeOrZero(BigDecimal current, BigDecimal baseline) {
        Double change = percentChange(current, baseline);
        return change != null ? change : 0.0;
    }

    public static Double percentChangeLongOrZero(long current, long baseline) {
        return percentChangeOrZero(BigDecimal.valueOf(current), BigDecimal.valueOf(baseline));
    }

    public static Double percentChangeLongOrZero(Long current, Long baseline) {
        long c = current != null ? current : 0L;
        long b = baseline != null ? baseline : 0L;
        return percentChangeOrZero(BigDecimal.valueOf(c), BigDecimal.valueOf(b));
    }

    public static String formatTzsLabel(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return String.format("TZS %,.0f", value);
    }

    public static String formatTonnesLabel(BigDecimal tonnes) {
        BigDecimal value = tonnes == null ? BigDecimal.ZERO : tonnes;
        return value.stripTrailingZeros().toPlainString() + " Tonnes";
    }

    /**
     * Ratio of {@code dayValue} to {@code monthPeak}; 0 when either is non-positive.
     */
    public static double intensityRatio(BigDecimal dayValue, BigDecimal monthPeak) {
        if (monthPeak == null || monthPeak.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        if (dayValue == null || dayValue.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return dayValue.divide(monthPeak, 4, RoundingMode.HALF_UP).doubleValue();
    }

    public static String intensityLevel(double intensity) {
        if (intensity <= 0.0) {
            return "NONE";
        }
        if (intensity <= 0.25) {
            return "LOW";
        }
        if (intensity <= 0.50) {
            return "MEDIUM";
        }
        if (intensity <= 0.75) {
            return "HIGH";
        }
        return "PEAK";
    }
}
