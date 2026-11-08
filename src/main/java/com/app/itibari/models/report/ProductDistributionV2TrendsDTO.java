package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Period-over-period trend for a product distribution row. Absolute values remain on
 * {@link ProductDistributionV2RowDTO}; this object carries percent deltas only.
 */
@Getter
@Setter
@Schema(description = "Trend vs the rolling previous period: same inclusive day count immediately before startDate.")
public class ProductDistributionV2TrendsDTO {

    @Schema(example = "vs Previous Period")
    private String trendLabel;

    @Schema(description = "Percent delta for unitsSold: (current - previous) / previous * 100. 0 when previous is zero.")
    private Double unitsSoldTrendPercentage;
}
