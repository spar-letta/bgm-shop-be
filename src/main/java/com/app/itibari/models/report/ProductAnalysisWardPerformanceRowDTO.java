package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Paged ward performance row for one product (customer.organization_location grouping).")
public class ProductAnalysisWardPerformanceRowDTO {

    @Schema(description = "1-based rank by settledTonnes descending (global across pages; matches fixed query order).")
    private int rank;

    private UUID organizationLocationPublicId;

    @Schema(description = "Organization location (ward) name, or UNKNOWN when missing.")
    private String organizationLocationName;

    @Schema(description = "Portal COGS: Σ(product.buying_price × quantity_delivered).")
    private BigDecimal totalCogsTzs;

    @Schema(description = "Settled tonnes: Σ(product.weight × quantity_delivered).")
    private BigDecimal settledTonnes;

    private long distinctCustomerCount;

    private long deliveredOrderCount;

    @Schema(description = "Portal AOV: totalCogsTzs ÷ deliveredOrderCount when orders > 0, else 0.")
    private BigDecimal averageOrderValueTzs;

    @Schema(description = "Trend vs previous comparable window (equal inclusive day count before startDate). "
            + "totalCogsTzsTrendPercentage and settledTonnesTrendPercentage are populated; "
            + "averageOrderValueTzsTrendPercentage is optional detail.")
    private ProductAnalysisWardPeriodTrendsDTO periodTrends;
}
