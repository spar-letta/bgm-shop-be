package com.app.itibari.models.report;

import com.app.itibari.dataTypes.CustomerLocationMarkerCategoryV2;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Schema(description = "Supplier Portal customer heat map cells grouped by category (NEW, ACTIVE, CHURNED). "
        + "INACTIVE customers are omitted. "
        + "Each entry is a geohash cell center with a customer count — exact customer coordinates are never returned.")
public class CustomerLocationsHeatMapV2DTO {

    private Map<CustomerLocationMarkerCategoryV2, List<CustomerLocationsHeatMapCellV2DTO>> points =
            new EnumMap<>(CustomerLocationMarkerCategoryV2.class);

    private CustomerLocationsHeatMapMetaV2DTO meta = new CustomerLocationsHeatMapMetaV2DTO();

    public CustomerLocationsHeatMapV2DTO() {
        points.put(CustomerLocationMarkerCategoryV2.NEW, new ArrayList<>());
        points.put(CustomerLocationMarkerCategoryV2.ACTIVE, new ArrayList<>());
        points.put(CustomerLocationMarkerCategoryV2.CHURNED, new ArrayList<>());
    }
}
