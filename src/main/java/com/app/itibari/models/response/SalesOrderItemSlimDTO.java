package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class SalesOrderItemSlimDTO {

    private UUID publicId;
    private int quantity;           // maps to SalesOrderItems.quantityDelivered (delivered qty)
    private int quantityDelivered;  // same as quantity — kept for explicit clarity
    private BigDecimal pricePerUnit;
    private ProductSlimDTO product;
}
