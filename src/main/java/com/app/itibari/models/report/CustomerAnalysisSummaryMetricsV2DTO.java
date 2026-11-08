package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Supplier Portal Customer Analysis V2 summary KPI cards.")
public class CustomerAnalysisSummaryMetricsV2DTO {

    @Schema(description = "COUNT(DISTINCT customers) with at least one DELIVERED supplier line "
            + "(quantity_delivered > 0) in the window. Matches Dashboard activeCustomers and "
            + "all-products activeCustomers for the same date + geography filters.")
    private TrendMetricDTO<Long> customers;

    @Schema(description = "Customers whose earliest delivered supplier-portal order "
            + "(MIN(so.creation_time) across qualifying DELIVERED lines in the filtered geography) falls within the requested window.")
    private TrendMetricDTO<Long> newCustomers;

    @Schema(description = "Average supplier portal COGS (TZS) per delivered order in the window: "
            + "Σ(product.buying_price × quantity_delivered) ÷ deliveredOrderCount. "
            + "COGS uses current products.buying_price — not frozen at order time.")
    private TrendMetricDTO<BigDecimal> aov;

    @Schema(description = "Distinct customers with at least two delivered supplier orders "
            + "(COUNT(DISTINCT so.public_id) >= 2) in the filtered window.")
    private TrendMetricDTO<Long> frequentCustomers;
}
