package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Period-over-period trend metadata for a district row. Absolute values remain on
 * {@link DistrictPerformanceV2RowDTO}; this object carries percent deltas only.
 */
@Getter
@Setter
@Schema(description = "Trend percentages vs the rolling previous period: same inclusive day count immediately before startDate (not calendar-month unless dates imply it).")
public class DistrictPerformanceV2TrendsDTO {

    @Schema(example = "vs Previous Period")
    private String trendLabel;

    @Schema(description = "Percent delta for supplierLineRevenueTzs: (current - previous) / previous * 100. 0 when previous is zero.")
    private Double supplierLineRevenueTzsTrendPercentage;

    @Schema(description = "Percent delta for settledTonnes. 0 when previous is zero.")
    private Double settledTonnesTrendPercentage;

    @Schema(description = "Percent delta for distinctCustomerCount. 0 when previous is zero.")
    private Double distinctCustomerCountTrendPercentage;

    @Schema(description = "Percent delta for deliveredOrderCount. 0 when previous is zero.")
    private Double deliveredOrderCountTrendPercentage;
}
