package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Ward-style ranking row (customer.organization_location). UI may label this 'district'.")
public class ProductAnalysisDistrictRankingRowDTO {

    private int rank;

    private UUID organizationLocationPublicId;

    @Schema(description = "Organization location (ward) name, or UNKNOWN when missing.")
    private String organizationLocationName;

    @Schema(description = "Settled tonnes (Total Units Sold) for the ward bucket: Σ(product.weight × quantity_delivered).")
    private BigDecimal settledTonnes;

    @Schema(description = "Portal COGS in the ward bucket (TZS): Σ(product.buying_price × quantity_delivered). Current buying_price — not frozen at order time.")
    private BigDecimal totalCogsTzs;

    @Schema(description = "Distinct delivered orders in the ward bucket (for AOV denominator).")
    private long deliveredOrderCount;

    @Schema(description = "Portal AOV: totalCogsTzs ÷ deliveredOrderCount when orders > 0, else 0.")
    private BigDecimal averageOrderValueTzs;

    @Schema(description = "Trend vs previous comparable window for settledTonnes and averageOrderValueTzs.")
    private ProductAnalysisWardPeriodTrendsDTO periodTrends;
}
