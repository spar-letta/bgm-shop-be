package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Single-day Product Analysis sales-trend bucket (COGS, orders, customers).")
public class ProductAnalysisDailyCogsPointDTO {

    private LocalDate date;

    @Schema(description = "Portal V2 COGS (TZS) for this date: Σ(product.buying_price × quantity_delivered). Current buying_price — not frozen at order time.")
    private BigDecimal totalCogsTzs;

    @Schema(description = "Distinct delivered orders on this date (all-products trend; 0 when not populated).")
    private long orderCount;

    @Schema(description = "Distinct customers with delivered supplier lines on this date (all-products trend; 0 when not populated).")
    private long customerCount;

    public ProductAnalysisDailyCogsPointDTO(LocalDate date, BigDecimal totalCogsTzs) {
        this(date, totalCogsTzs, 0L, 0L);
    }
}
