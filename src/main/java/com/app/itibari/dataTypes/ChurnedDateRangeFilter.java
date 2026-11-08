package com.app.itibari.dataTypes;

public enum ChurnedDateRangeFilter {
    Range_30_120("30–120 days", 30, 120),
    Range_30_59("30–59 days", 30, 59),
    Range_60_89("60–89 days", 60, 89),
    Range_90_120("90–120 days", 90, 120);

    private final String displayName;
    private final int minDays;
    private final int maxDays;

    ChurnedDateRangeFilter(String displayName, int minDays, int maxDays) {
        this.displayName = displayName;
        this.minDays = minDays;
        this.maxDays = maxDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinDays() {
        return minDays;
    }

    public int getMaxDays() {
        return maxDays;
    }
}