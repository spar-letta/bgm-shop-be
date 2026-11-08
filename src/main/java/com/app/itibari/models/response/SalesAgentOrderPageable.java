package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SalesAgentOrderPageable {
    private long totalElements;
    private long pageSize;
    private long totalPages;
    private boolean last;
    private boolean first;
    private long pageNumber;
    private String agent;
    private String from;
    private String to;
    private List<SalesOrderResponse> content;
    private BigDecimal totalAmount;
}
