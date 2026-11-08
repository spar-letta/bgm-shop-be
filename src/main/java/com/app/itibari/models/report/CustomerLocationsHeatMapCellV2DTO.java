package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Privacy-preserving heat-map cell: geohash cell center and customer count (not exact customer coordinates).")
public class CustomerLocationsHeatMapCellV2DTO {

    @Schema(description = "Latitude of the geohash cell center.")
    private double lat;

    @Schema(description = "Longitude of the geohash cell center.")
    private double lng;

    @Schema(description = "Number of distinct customers aggregated into this cell for the category.")
    private int count;
}
