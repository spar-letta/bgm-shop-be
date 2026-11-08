package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class InventoryItemRequest {

    private UUID inventoryItemPublicId;

    private Integer closingStock;
}
