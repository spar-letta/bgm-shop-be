package com.app.itibari.models.report;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCustomerPerformanceDTO {
    private String customerName;
    private Long numberOfOrders;
    private BigDecimal aov;
}
