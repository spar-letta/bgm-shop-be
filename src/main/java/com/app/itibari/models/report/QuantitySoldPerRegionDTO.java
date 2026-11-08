package com.app.itibari.models.report;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QuantitySoldPerRegionDTO {
    private String regionName;
    private Long quantitySold;
    private BigDecimal weightSold;
}
