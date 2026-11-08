package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Ward-level COGS bucket for Customer Analysis district-cogs. UI may label this 'district'.")
public class CustomerAnalysisDistrictCogsRowV2DTO {

    private UUID organizationLocationPublicId;

    @Schema(description = "Organization location (ward) name, or UNKNOWN when missing.")
    private String organizationLocationName;

    @Schema(description = "Portal COGS in the ward bucket: Σ(product.buying_price × quantity_delivered).")
    private BigDecimal totalCogsTzs;
}
