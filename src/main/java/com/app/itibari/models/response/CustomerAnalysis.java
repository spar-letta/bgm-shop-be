package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CustomerAnalysis {

    private long totalOrders;

    private String customerName;

    private String customerPhone;

    private BigDecimal totalAmount;

    private Integer registrationCount;
}
