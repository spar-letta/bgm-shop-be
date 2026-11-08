package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Operational ward-style bucket keyed from {@link com.app.itibari.models.entities.Customers#getOrganizationLocation()}.
 * Treat as provisional geography until/if a curated district hierarchy is modeled.
 */
@Getter
@Setter
@Schema(description = "Supplier portal V2 district-style performance row (customer.organizationLocation provisional grouping).")
public class DistrictPerformanceV2RowDTO {

    /**
     * Null when customers have no organization location FK (rollup still names the bucket UNKNOWN via display name).
     */
    private UUID organizationLocationPublicId;

    @Schema(description = "Organization location ward name, or UNKNOWN when missing.")
    private String organizationLocationName;

    @Schema(description = "COGS (TZS): Σ(product.buying_price × quantityDelivered). Current buying_price — not frozen at order time.")
    private BigDecimal supplierLineRevenueTzs;

    @Schema(description = "Σ(weight × quantityDelivered); product.weight stored in tonnes.")
    private BigDecimal settledTonnes;

    private long distinctCustomerCount;

    private long deliveredOrderCount;

    /**
     * Optional period trends vs the immediately preceding window (same rule as summary-metrics).
     * Null only if enrichment is skipped; populated by {@code district-performance-v2} when trends are computed.
     */
    @Schema(description = "Trend percentages vs previous period of equal inclusive length. Additive; does not replace scalar fields above.")
    private DistrictPerformanceV2TrendsDTO periodTrends;
}
