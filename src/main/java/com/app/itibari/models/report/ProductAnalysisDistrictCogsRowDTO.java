package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Ward-style COGS bucket (customer.organization_location). UI may label this 'district'.")
public class ProductAnalysisDistrictCogsRowDTO {

    private UUID organizationLocationPublicId;

    @Schema(description = "Organization location (ward) name, or UNKNOWN when missing.")
    private String organizationLocationName;

    @Schema(description = "1-based rank under descending totalCogsTzs in the returned slice.")
    private int rank;

    @Schema(description = "Portal COGS for this product in the ward bucket: Σ(product.buying_price × quantity_delivered).")
    private BigDecimal totalCogsTzs;

    @Schema(description = "Trend vs previous comparable window for totalCogsTzs.")
    private ProductAnalysisWardPeriodTrendsDTO periodTrends;
}
