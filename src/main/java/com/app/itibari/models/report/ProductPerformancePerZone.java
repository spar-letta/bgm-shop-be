package com.app.itibari.models.report;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductPerformancePerZone {
    private BigDecimal orderValueInThousands;
    private String zoneName;
    private Long unitsSold;
}
