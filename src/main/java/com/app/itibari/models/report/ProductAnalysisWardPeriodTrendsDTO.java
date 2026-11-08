package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Period-over-period trend percentages for Product Analysis ward rows vs the immediately preceding
 * equal-length inclusive window ({@link com.app.itibari.services.supplierportal.SupplierPortalDashboardMath#inclusivePreviousWindow}).
 */
@Getter
@Setter
@Schema(description = "Trend percentages vs previous comparable window. Null percent when baseline is zero (Product Analysis family).")
public class ProductAnalysisWardPeriodTrendsDTO {

    @Schema(example = "vs Previous Period")
    private String trendLabel;

    @Schema(description = "Percent delta for portal COGS (Σ(product.buying_price × quantity_delivered)).")
    private Double totalCogsTzsTrendPercentage;

    @Schema(description = "Percent delta for settled tonnes (Σ product.weight × quantity_delivered).")
    private Double settledTonnesTrendPercentage;

    @Schema(description = "Percent delta for portal AOV (COGS ÷ distinct delivered orders in ward bucket).")
    private Double averageOrderValueTzsTrendPercentage;
}
