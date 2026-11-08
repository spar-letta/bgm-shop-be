package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ApprovedProductItems {

    private String productId;

    private boolean approved;

    private BigDecimal approvedAmount;   // approved discount amount per unit
}
