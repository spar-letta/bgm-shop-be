package com.app.itibari.models.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
public class OrderStats {

    private long orderCount;

    private BigDecimal averageOrderValue;
}
