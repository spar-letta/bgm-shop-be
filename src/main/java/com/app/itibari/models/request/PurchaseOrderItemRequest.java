package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class PurchaseOrderItemRequest {

    private UUID productPublicId;

    private Integer quantity;

    private BigDecimal unitPrice;
}
