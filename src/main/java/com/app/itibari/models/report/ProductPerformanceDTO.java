package com.app.itibari.models.report;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Month;

@Getter
@Setter
public class ProductPerformanceDTO {
    private Long productId;
    private String productName;
    private Long unitsSold;
    private BigDecimal orderValueInThousands;
    private Month month;
    private Integer year;
}
