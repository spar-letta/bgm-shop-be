package com.app.itibari.models.request;

import com.app.itibari.dataTypes.AdjustmentType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProductAdjustmentRequest {

    private int quantity;

    private String reason;

    private AdjustmentType adjustmentType;

    private UUID warehousePublicId;
}
