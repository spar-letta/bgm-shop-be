package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Customer Analysis V2 daily delivered-order trend over the requested calendar month.")
public class CustomerAnalysisOrdersTrendV2DTO {

    @Schema(description = "One point per calendar day in the requested range (zero-filled when no orders).")
    private List<CustomerAnalysisDailyOrderPointDTO> series;

    @Schema(description = "Total distinct delivered orders in the calendar month (sum of series points; same semantics as each daily bucket). "
            + "Compared to the previous comparable window (same length, immediately preceding). "
            + "trendPercentage is null when the previous-window baseline is zero.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private TrendMetricDTO<Long> headline;
}
