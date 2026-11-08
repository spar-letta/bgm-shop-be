package com.app.itibari.models.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Paginated portal row; {@code rank} is {@code pageOffset + rowIndex + 1} under stable descending delivered-units ordering.
 */
@Getter
@Setter
@Schema(description = "Supplier portal V2 product distribution row (units sold, tonnes, COGS, optional thumbnail). COGS = Σ(product.buying_price × quantity_delivered); current buying_price, not frozen at order time.")
public class ProductDistributionV2RowDTO {

    @Schema(description = "1-based leaderboard position spanning pages (ordering fixed to unitsSold DESC).")
    private int rank;

    private UUID productPublicId;

    private String productName;

    private String sku;

    private String uom;

    @Schema(description = "Delivered quantity (sum of line quantityDelivered).")
    private long unitsSold;

    @Schema(description = "Σ (product.weight × quantityDelivered); weights are persisted in tonnes (no kg→tonne divisor).")
    private BigDecimal settledTonnes;

    @Schema(description = "Supplier-scoped COGS (TZS): Σ(product.buying_price × quantityDelivered).")
    private BigDecimal supplierLineRevenueTzs;

    @Schema(description = "Resolved from FileService PRODUCT images when available for this page's distinct products.")
    private String productImageUrl;

    /** Alias for web UIs that read {@code imageUrl} (same as POS inventory ProductResponse). */
    @JsonProperty("imageUrl")
    public String imageUrl() {
        return productImageUrl;
    }

    @Schema(description = "Trend vs previous period of equal inclusive length (units sold). Populated by product-distribution-v2.")
    private ProductDistributionV2TrendsDTO periodTrends;
}
