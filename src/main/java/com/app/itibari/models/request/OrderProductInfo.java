package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderProductInfo {
    private int quantity;
    private boolean hasDiscount;
    private BigDecimal volumeDiscount = BigDecimal.ZERO;
    private BigDecimal specialDiscount = BigDecimal.ZERO;
}

