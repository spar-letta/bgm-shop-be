package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerOrderDTO {

    private LocalDateTime purchaseDate;

    private String customerName;

    private String region;

    private String location;

    private String productName;

    private String skuCode;

    private Long quantityPurchased;

    private String salesOrderNumber;
    private String address;
}
