package com.app.itibari.models.report;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductSaleDistributionDTO {
    private String productName;
    private String sku;
    private String uom;
    private BigDecimal orderValue;
    private Integer quantity;
}
