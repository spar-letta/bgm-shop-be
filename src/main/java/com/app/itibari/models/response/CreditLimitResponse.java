package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class CreditLimitResponse {
    private boolean hasCreditLimit;
    private BigDecimal creditLimit;
    private boolean hasActiveOrder;
    private boolean hasOverduePayment;
    private BigDecimal creditInterestRate;
    private Integer repaymentTermInDays;
    private boolean onBoardedToBNPL;
}
