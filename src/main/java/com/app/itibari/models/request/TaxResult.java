package com.app.itibari.models.request;

import java.math.BigDecimal;

public record TaxResult(BigDecimal taxAmount, BigDecimal amountAfterTax) {
}
