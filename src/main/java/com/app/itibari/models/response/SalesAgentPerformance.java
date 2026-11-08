package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SalesAgentPerformance {

    private long totalElements;

    private long pageSize;

    private long totalPages;

    private boolean last;

    private boolean first;

    private long pageNumber;

    private List<?> content;

    private Integer totalSales;

    private BigDecimal totalSalesAmount;

    private Integer totalOrders;

    private BigDecimal totalCustomerRegistration;
}
