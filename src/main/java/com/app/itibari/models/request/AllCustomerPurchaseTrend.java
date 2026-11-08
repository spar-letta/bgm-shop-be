package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AllCustomerPurchaseTrend {
    private BigDecimal totalRevenue;
    private Integer year;
    private Integer month;
    private String label;
}
