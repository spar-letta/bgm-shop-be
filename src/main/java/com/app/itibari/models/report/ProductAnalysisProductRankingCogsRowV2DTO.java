package com.app.itibari.models.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Product Analysis V2 product row ranked by portal COGS (supplier-wide all-products scope).")
public class ProductAnalysisProductRankingCogsRowV2DTO {

    @Schema(description = "1-based rank under fixed descending totalCogsTzs order (spans pages).")
    private int rank;

    private UUID productPublicId;

    private String productName;

    @Schema(description = "Product unit of measure from warehouse catalog (products.unit_of_measure), e.g. Bag or Carton.")
    private String unitOfMeasure;

    @Schema(description = "Total units sold: Σ quantity_delivered (whole units).")
    private BigDecimal totalQuantitySoldUnits;

    @Schema(description = "Portal COGS (TZS): Σ(product.buying_price × quantity_delivered).")
    private BigDecimal totalCogsTzs;

    @Schema(description = "Comma-formatted COGS label for display.", example = "TZS 1,250,000")
    private String totalCogsTzsLabel;

    @Schema(description = "Resolved from FileService PRODUCT images when available; omitted when none.")
    private String productImageUrl;

    @JsonProperty("imageUrl")
    public String imageUrl() {
        return productImageUrl;
    }

    @Schema(description = "Trend vs previous comparable window for totalCogsTzs (ranking metric).")
    private ProductAnalysisWardPeriodTrendsDTO periodTrends;
}
