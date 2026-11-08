package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Schema(description = "Product Analysis V2 ward COGS ranking for one product (chart/table feed).")
public class ProductAnalysisDistrictCogsV2DTO {

    @Schema(description = "Top ward slices ordered by portal COGS descending (limit applied).")
    private List<ProductAnalysisDistrictCogsRowDTO> rows;

    @Schema(description = "Total portal COGS (TZS) for the product across all wards in the window (donut center total): "
            + "Σ(product.buying_price × quantity_delivered). "
            + "Not limited to returned rows. Current buying_price — not frozen at order time.")
    private BigDecimal totalCogsTzs;
}
