package com.app.itibari.models.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RegionWarehouseMappingRequest {

    @NotNull
    private UUID warehousePublicId;

    private List<UUID> regionPublicIds;
}
