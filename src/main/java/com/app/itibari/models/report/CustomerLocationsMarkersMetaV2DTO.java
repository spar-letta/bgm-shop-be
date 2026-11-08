package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Marker cap and omission metadata for customer locations V2.")
public class CustomerLocationsMarkersMetaV2DTO {

    private int returnedCount;

    private int omittedInvalidCoordinates;

    private boolean truncated;

    private int maxMarkers;
}
