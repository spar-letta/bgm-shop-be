package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CustomerPurchaseTrend {
    private BigDecimal totalRevenue;
    private Integer year;
    private Integer month;
    private String label;
}
