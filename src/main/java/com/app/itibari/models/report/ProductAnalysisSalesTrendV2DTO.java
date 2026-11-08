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
@Schema(description = "Product Analysis V2 daily sales trend over the requested calendar month (first through last day).")
public class ProductAnalysisSalesTrendV2DTO {

    @Schema(description = "One point per calendar day (zero-filled). Each point includes COGS, orderCount, and customerCount for graph series.")
    private List<ProductAnalysisDailyCogsPointDTO> series;

    @Schema(description = "Always present. Total portal COGS in the requested window vs the previous comparable window (same length, immediately preceding).",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private TrendMetricDTO<BigDecimal> headline;
}
