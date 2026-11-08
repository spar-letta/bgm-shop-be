package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class DeliverVanSaleItemRequest {

    private UUID routePublicId;

    private List<ItemRequest> items = new ArrayList<>();

    private boolean existingCustomer;

    private UUID customerPublicId;

    private CustomersRequest customersRequest;
}
