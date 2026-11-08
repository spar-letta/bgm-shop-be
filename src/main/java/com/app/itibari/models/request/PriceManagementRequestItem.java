package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class PriceManagementRequestItem {

    private String productName;

    private String sku;

    private BigDecimal price = BigDecimal.ZERO;

    private UUID productId;
}
