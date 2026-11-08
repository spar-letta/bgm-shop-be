package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreditLimitUtilizationResponse {
    private BigDecimal creditLimit = new BigDecimal("0.00");
    private BigDecimal utilizedAmount = new BigDecimal("0.00");
    private BigDecimal utilizationPercentage = new BigDecimal("0.00");
    private BigDecimal availableCreditLimit = new BigDecimal("0.00");
}
