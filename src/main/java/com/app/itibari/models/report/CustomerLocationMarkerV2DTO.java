package com.app.itibari.models.report;

import com.app.itibari.dataTypes.CustomerLocationMarkerCategoryV2;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "Lightweight map marker for Supplier Portal customer locations V2. "
        + "latitude/longitude are geohash cell centers (precision 5), not exact customer coordinates.")
public class CustomerLocationMarkerV2DTO {

    private UUID customerPublicId;

    private String customerName;

    @Schema(description = "Geohash cell-center latitude (precision 5; not the exact customer latitude).")
    private Double latitude;

    @Schema(description = "Geohash cell-center longitude (precision 5; not the exact customer longitude).")
    private Double longitude;

    @Schema(description = "NEW, ACTIVE, INACTIVE, or CHURNED (churn override when last supplier delivery >30 days ago).")
    private CustomerLocationMarkerCategoryV2 category;

    private UUID regionPublicId;

    private String regionName;

    private UUID organizationLocationPublicId;

    private String organizationLocationName;
}
