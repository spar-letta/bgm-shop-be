package com.app.itibari.models.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ReconRouteSummary(int salesOrderSize,
                                long salesOrdersDeliveredSize,
                                BigDecimal totalDiscount,
                                BigDecimal totalDeposit,
                                BigDecimal valuePicked,
                                BigDecimal valueSold,
                                BigDecimal totalExpenses,
                                BigDecimal totalCredit,
                                BigDecimal balanceValue) {
}
