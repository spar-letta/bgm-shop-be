package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Ward-level customer activity bucket. UI may label this 'district'; grouped by customer.organization_location.")
public class CustomerAnalysisDistrictRankingRowV2DTO {

    private int rank;

    private UUID organizationLocationPublicId;

    @Schema(description = "Organization location (ward) name, or UNKNOWN when missing.")
    private String organizationLocationName;

    private long distinctCustomerCount;

    private long deliveredOrderCount;

    @Schema(description = "Portal AOV in the ward bucket: Σ(product.buying_price × quantity_delivered) ÷ deliveredOrderCount when orders > 0, else 0.")
    private BigDecimal averageOrderValueTzs;

    @Schema(description = "Share of total averageOrderValueTzs across returned rows: row value ÷ sum(rows) × 100. 0 when total is zero.")
    private Double averageOrderValueTzsPercentageOfTotal;
}
