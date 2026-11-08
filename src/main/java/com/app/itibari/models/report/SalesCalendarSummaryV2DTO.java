package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Left-side summary cards for the supplier portal sales calendar heat map.")
public class SalesCalendarSummaryV2DTO {

    @Schema(description = "Day with highest supplier COGS (buying_price × quantity_delivered) in the selected month; null when the month has no sales.")
    private LocalDate peakDay;

    @Schema(description = "Peak-day supplier COGS (buying_price × quantity_delivered) in TZS.")
    private BigDecimal peakDayRevenueTzs;

    @Schema(description = "Formatted peak-day revenue label.", example = "TZS 2,400,000")
    private String peakDayRevenueLabel;

    @Schema(description = "Total settled tonnes sold in the selected month.")
    private BigDecimal unitsSoldTonnes;

    @Schema(description = "Formatted month total tonnes label.", example = "48.5 t")
    private String unitsSoldLabel;

    @Schema(description = "Total distinct delivered orders in the selected month.")
    private long totalOrders;

    @Schema(description = "Month supplier COGS (buying_price × quantity_delivered) vs the immediately preceding calendar month.")
    private TrendMetricDTO<BigDecimal> monthlyRevenueVersusPreviousMonth;

    @Schema(description = "Month delivered-order count vs the immediately preceding calendar month.")
    private TrendMetricDTO<Long> monthlyOrdersVersusPreviousMonth;

    @Schema(description = "Month settled tonnes sold vs the immediately preceding calendar month.")
    private TrendMetricDTO<BigDecimal> monthlyUnitsSoldVersusPreviousMonth;
}
