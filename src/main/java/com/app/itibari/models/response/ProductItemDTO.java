package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProductItemDTO {

    private UUID productItemPublicId;

    private String productName;

    private String productCode;

    private String dimension;

    private BigDecimal price;

    private long quantity;

    private BigDecimal discount; // all aggregated discounts
}
