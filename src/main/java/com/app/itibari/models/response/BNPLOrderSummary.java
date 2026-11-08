package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class BNPLOrderSummary {
    private List<BNPLProducts> productsList;
    private BigDecimal discount;
    private BigDecimal total;
}
