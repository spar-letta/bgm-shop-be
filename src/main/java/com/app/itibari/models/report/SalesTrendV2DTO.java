package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Supplier Portal V2 sales trend: bucketed COGS series (Σ product.buying_price × quantity_delivered; current buying_price, not frozen at order time) plus YoY headline metric.")
public class SalesTrendV2DTO {

    @Schema(description = "Bucketed totals; granularity depends on monthly/quarterly/yearly flags.")
    private List<SalesRevenueDTO> series;

    @Schema(description = "Month-aligned COGS window compared to the same calendar window one year earlier.")
    private TrendMetricDTO<BigDecimal> monthToDateCogsVersusLastYear;
}
