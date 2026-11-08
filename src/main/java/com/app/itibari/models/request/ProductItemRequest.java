package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProductItemRequest {

    private UUID productItemPublicId;

    private long quantitySold;

    private long quantityReturned;

    private long quantityMissing;

    private long quantity;

    private UUID missingRejectionPublicId;
}
