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
@Schema(description = "Peak COGS day within a Product Analysis sales-calendar month.")
public class ProductAnalysisSalesCalendarPeakDayV2DTO {

    @Schema(description = "Calendar date of the peak day.")
    private LocalDate date;

    @Schema(description = "Portal COGS (TZS) on the peak day: Σ(product.buying_price × quantity_delivered).")
    private BigDecimal totalCogsTzs;

    @Schema(description = "Peak-day portal COGS vs the peak-day COGS in the immediately preceding calendar month.")
    private TrendMetricDTO<BigDecimal> totalCogsTzsVersusPreviousMonth;
}
