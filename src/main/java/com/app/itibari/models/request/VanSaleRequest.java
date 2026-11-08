package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class VanSaleRequest {

    private UUID deliveryAgentPublicId;

    private List<ProductItemRequest> productItemRequestList = new ArrayList<>();
}
