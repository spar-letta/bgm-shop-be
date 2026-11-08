package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Product Analysis V2 sales calendar heat map: daily COGS cells plus month summary cards.")
public class ProductAnalysisSalesCalendarV2DTO {

    @Schema(description = "Calendar month (1–12).")
    private int month;

    @Schema(description = "Calendar year.")
    private int year;

    @Schema(description = "One cell per calendar day in the month (zero-filled when no sales).")
    private List<ProductAnalysisDailyCogsPointDTO> days;

    @Schema(description = "Day with highest portal COGS in the month; null when the month has no sales. Ties resolve to the earliest day.")
    private ProductAnalysisSalesCalendarPeakDayV2DTO peakDay;

    @Schema(description = "Distinct delivered orders in the month for the filtered scope.")
    private long totalOrders;

    @Schema(description = "Settled tonnes sold in the month: Σ(product.weight × quantity_delivered).")
    private BigDecimal totalQuantitySoldTonnes;

    @Schema(description = "Total portal COGS in the month: Σ(product.buying_price × quantity_delivered).")
    private BigDecimal totalCogsTzs;

    @Schema(description = "Month delivered-order count vs the immediately preceding calendar month.")
    private TrendMetricDTO<Long> monthlyOrdersVersusPreviousMonth;

    @Schema(description = "Month settled tonnes sold vs the immediately preceding calendar month.")
    private TrendMetricDTO<BigDecimal> monthlyUnitsSoldVersusPreviousMonth;
}
