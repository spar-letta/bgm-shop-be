package com.app.itibari.models.report;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StatisticsDTO {
    private Long totalOrders;
    private BigDecimal totalTonnes;
    private Long activeCustomers;
    private BigDecimal revenue;
}
