package com.app.itibari.models.response;

import java.time.LocalDateTime;
import java.util.List;

public record CustomerOrderDTORapper(String routeId, String customerName, String region, String location,
                                     String address, List<InnerCustomerDetails> innerCustomerDetailsList) {

    public record InnerCustomerDetails(String productName, String skuCode, Long quantityPurchased,
                                       LocalDateTime purchaseDate){}
}
