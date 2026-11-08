package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DiscountRangeDTO {

    private BigDecimal discountAmount;

    private Integer minPurchaseQuantity;

    private Integer maxPurchaseQuantity;
}
