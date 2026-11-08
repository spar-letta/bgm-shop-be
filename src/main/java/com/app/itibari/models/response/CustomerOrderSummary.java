package com.app.itibari.models.response;

import java.util.List;

public record CustomerOrderSummary(
        String customerName,
        String routeId,
        String region,
        String location,
        String address,
        List<CustomerOrderDTORapper.InnerCustomerDetails> sortedOrders
) {}
