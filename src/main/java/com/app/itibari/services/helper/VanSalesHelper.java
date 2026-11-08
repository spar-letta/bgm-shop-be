package com.app.itibari.services.helper;

import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.ProductItem;
import com.app.itibari.models.entities.RoutePlanning;
import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.models.request.*;
import com.app.itibari.services.SalesOrdersService;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class VanSalesHelper {
    private final SalesOrdersService salesOrdersService;

    public VanSalesHelper(SalesOrdersService salesOrdersService) {
        this.salesOrdersService = salesOrdersService;
    }

    public void createSpecialDiscountRequest(Customers customers, RoutePlanning routePlanning, @NotEmpty List<VanItemRequest> items, List<ProductItem> productItems) {
        SpecialRequest specialRequest = new SpecialRequest();
        specialRequest.setCustomerId(customers.getPublicId());
        specialRequest.setItems(items.stream().map(item -> {
            RequestItemDTO requestItemDTO = new RequestItemDTO();

            ProductItem productItem = productItems.stream().filter(p -> p.getPublicId().equals(item.getProductItemPublicId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Product not found for publicId: " + item.getProductItemPublicId()));
            requestItemDTO.setProductPublicId(productItem.getProductPublicId());
            requestItemDTO.setQuantity(item.getQuantitySold());
            requestItemDTO.setSku(productItem.getProductCode());
            requestItemDTO.setProductName(productItem.getProductName());
            requestItemDTO.setUnitPrice(productItem.getProductSellingPrice());
            requestItemDTO.setTotalDiscountRequest(item.getDiscountPerUnit());
            requestItemDTO.setProductItemPublicId(productItem.getPublicId().toString());
            return requestItemDTO;
        }).toList());
        specialRequest.setHasDeliveryAgent(true);
        specialRequest.setDeliveryAgent(routePlanning.getDeliveryAgentName());
        specialRequest.setDeliveryAgentPublicId(routePlanning.getDeliveryAgentId());
        salesOrdersService.requestForSpecialDiscount(specialRequest, null);
    }
    public SalesOrders createVanSalesOrder(Customers customers, RoutePlanning routePlanning, @NotEmpty List<VanItemRequest> items, List<ProductItem> productItems) {

        // create an order and adds to route

        Set<SalesOrderItemsRequest> salesOrderItemsRequests = new HashSet<>();
        items.forEach(item -> {
            ProductItem productItem = productItems.stream().filter(p -> p.getPublicId().equals(item.getProductItemPublicId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Product not found for publicId: " + item.getProductItemPublicId()));
            SalesOrderItemsRequest salesOrderItemsRequest = new SalesOrderItemsRequest();
            salesOrderItemsRequest.setProductId(productItem.getProductPublicId());
            salesOrderItemsRequest.setQuantity(item.getQuantitySold());
            salesOrderItemsRequests.add(salesOrderItemsRequest);
        });
        SalesOrdersRequest salesOrdersRequest = SalesOrdersRequest.builder()

                .sourceChannel("VANSALES")
                .customerId(customers.getPublicId())
                .bookedBy(routePlanning.getDeliveryAgentName())
                .bookedBySalesAgent(false)
                .salesOrderItems(salesOrderItemsRequests)
                .build();


        return salesOrdersService.createVanSalesOrder(salesOrdersRequest,routePlanning);

    }
}
