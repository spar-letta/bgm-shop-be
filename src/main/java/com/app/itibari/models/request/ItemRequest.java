package com.app.itibari.models.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ItemRequest {

    private UUID productItemPublicId;

    private int quantitySold;

    private UUID productReconItemPublicId;

    public ItemRequest(UUID productPublicId, int quantitySold, UUID productReconItemPublicId) {

        this.productItemPublicId = productPublicId;
        this.quantitySold = quantitySold;
        this.productReconItemPublicId = productReconItemPublicId;
    }
}
