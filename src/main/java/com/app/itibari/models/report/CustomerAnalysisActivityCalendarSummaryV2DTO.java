package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Customer Analysis activity-calendar month summary cards (additive to days[]).")
public class CustomerAnalysisActivityCalendarSummaryV2DTO {

    @Schema(description = "Day with highest distinct-customer count in the month; null when the month has no activity. No trend.")
    private CustomerAnalysisActivityCalendarPeakDayV2DTO peakDay;

    @Schema(description = "Distinct delivered orders in the selected calendar month (sum of per-day distinct order counts).")
    private TrendMetricDTO<Long> totalOrders;

    @Schema(description = "Settled quantity sold in the selected calendar month: Σ(product.weight × quantity_delivered) "
            + "on delivered supplier-attributed lines (product.weight stored in tonnes).")
    private TrendMetricDTO<BigDecimal> unitsSold;
}
