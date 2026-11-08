package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryRegionDistributionDTO {
    private String region;
    private Long quantity = 0L;
}

