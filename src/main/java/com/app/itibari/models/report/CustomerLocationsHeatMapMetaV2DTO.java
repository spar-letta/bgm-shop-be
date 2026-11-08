package com.app.itibari.models.report;

import com.app.itibari.dataTypes.CustomerLocationMarkerCategoryV2;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumMap;
import java.util.Map;

@Getter
@Setter
@Schema(description = "Heat map cell cap and per-category customer-count metadata for customer locations V2.")
public class CustomerLocationsHeatMapMetaV2DTO {

    @Schema(description = "Number of geohash cells returned across all categories.")
    private int returnedCount;

    @Schema(description = "True when the underlying snapshot row set hit the soft input cap before aggregation.")
    private boolean truncated;

    @Schema(description = "Soft input row cap applied before geohash aggregation.")
    private int maxPoints;

    private Map<CustomerLocationMarkerCategoryV2, Long> countByCategory =
            new EnumMap<>(CustomerLocationMarkerCategoryV2.class);
}
