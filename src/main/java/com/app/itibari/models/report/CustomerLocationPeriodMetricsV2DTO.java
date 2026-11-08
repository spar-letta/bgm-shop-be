package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Date-scoped delivered-order metrics for a single customer (Supplier Portal V2 semantics).")
public class CustomerLocationPeriodMetricsV2DTO {

    private LocalDate startDate;

    private LocalDate endDate;

    private long deliveredOrderCount;

    @Schema(description = "Portal line COGS (TZS): Σ(product.buying_price × quantity_delivered).")
    private BigDecimal totalCogsTzs;

    @Schema(description = "Settled tonnes: Σ(product.weight × quantity_delivered).")
    private BigDecimal settledTonnes;

    private LocalDate lastOrderDate;
}
