package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ProductReconAction {

    private UUID productReconPublicId;

    private List<ProductItemRequest> productItems;
}
