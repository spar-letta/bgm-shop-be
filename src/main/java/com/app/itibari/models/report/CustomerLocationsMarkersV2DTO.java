package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Schema(description = "Unpaged Supplier Portal customer map markers with cap metadata.")
public class CustomerLocationsMarkersV2DTO {

    private List<CustomerLocationMarkerV2DTO> markers = new ArrayList<>();

    private CustomerLocationsMarkersMetaV2DTO meta = new CustomerLocationsMarkersMetaV2DTO();
}
