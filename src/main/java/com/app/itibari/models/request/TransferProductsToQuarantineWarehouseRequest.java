package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class TransferProductsToQuarantineWarehouseRequest {

    private UUID reasonPublicId;

    private String reasonDescription;

    private List<ProductItemRequest> productItems = new ArrayList<>();
}
