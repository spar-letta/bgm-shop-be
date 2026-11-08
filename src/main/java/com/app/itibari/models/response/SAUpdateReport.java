package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SAUpdateReport {

    private LocalDate date;

    private String zone;

    private String salesAgent;

    private BigDecimal totalGMV;

    private BigDecimal palmOil;

    private BigDecimal sunflowerOil;

    private BigDecimal sugar;

    private BigDecimal detergents;

    private BigDecimal pasta;

    private BigDecimal beverages;

    private BigDecimal babyCare;

    private BigDecimal hygiene;

    private BigDecimal other;

    private BigDecimal food;

    private BigDecimal vegetable;

    private BigDecimal soap;
}
