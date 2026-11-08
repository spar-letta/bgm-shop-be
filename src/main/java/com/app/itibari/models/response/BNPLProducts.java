package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BNPLProducts {
    private String salesOrderItemPublicId;
    private BigDecimal amount;
    private String sku;
    private String productName;
}
