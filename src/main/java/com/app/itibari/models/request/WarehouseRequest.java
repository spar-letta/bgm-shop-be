package com.app.itibari.models.request;

import com.app.itibari.dataTypes.WarehouseType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class WarehouseRequest {

    @NotNull
    private String warehouseName;

    private String warehouseStreet;

    private String warehouseCity;

    private String warehouseState;

    private String warehousePostalCode;

    private WarehouseType warehouseType;

    private String latitude;

    private String longitude;

    private UUID organizationRegionId;

    private UUID mainWarehouseId;

    private boolean coreWarehouse;
}
