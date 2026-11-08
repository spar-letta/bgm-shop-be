package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * V2 Supplier Portal metric envelope: absolute value plus trend metadata vs an explicit baseline rule.
 *
 * <p>{@code trendPercentage} uses {@code (current - baseline) / baseline * 100}. When baseline is absent or
 * zero, {@code trendPercentage} is null — callers treat this as undefined.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current metric value plus period-over-period percent change.")
public class TrendMetricDTO<T> {

    @Schema(description = "Value for the user-selected reporting window.")
    private T value;

    @Schema(description = "Percent delta vs baseline: (current - baseline) / baseline * 100.")
    private Double trendPercentage;

    /**
     * UX label naming the baseline, e.g. {@code vs Previous Period} or {@code vs Last Year}.
     */
    @Schema(example = "vs Previous Period")
    private String trendLabel;
}
