package com.app.itibari.models.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Supplier Portal Product Analysis V2 summary cards (single-product or all-products scope).")
public class ProductAnalysisSummaryMetricsV2DTO {

    @Schema(description = "COUNT(DISTINCT customers) with at least one DELIVERED supplier line "
            + "(quantity_delivered > 0). All-products scope matches Dashboard activeCustomers and "
            + "customer-analysis customers; product-scoped summary may be lower.")
    private TrendMetricDTO<Long> activeCustomers;

    @Schema(description = "Distinct delivered orders in the filtered scope (retained for compatibility).")
    private TrendMetricDTO<Long> totalOrders;

    @Schema(description = "Settled tonnes: SUM(product.weight × quantity_delivered); weight stored in tonnes.")
    private TrendMetricDTO<BigDecimal> totalQuantitySoldTonnes;

    @Schema(description = "Portal V2 COGS (TZS): SUM(product.buying_price × quantity_delivered). "
            + "Uses current products.buying_price — not a sell-price snapshot from the order item.")
    private TrendMetricDTO<BigDecimal> totalCogsTzs;

    @Schema(description = "Average order value (TZS): portal COGS ÷ distinct delivered order count in the same filtered scope.")
    private TrendMetricDTO<BigDecimal> averageOrderValueTzs;

    @Schema(description = "Product thumbnail URL when available (single-product summary only). Null when no image or lookup fails.")
    private String productImageUrl;

    @JsonProperty("imageUrl")
    public String imageUrl() {
        return productImageUrl;
    }

    @Schema(description = "1-based best-selling rank by portal COGS among supplier products in the filtered window "
            + "(same ordering as product-ranking-cogs). Null when the product has no qualifying delivered activity in the window.")
    private Integer ranking;
}
