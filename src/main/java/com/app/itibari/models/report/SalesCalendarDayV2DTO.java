package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Single calendar day in the supplier portal sales heat map.")
public class SalesCalendarDayV2DTO {

    @Schema(description = "Calendar date (ISO-8601).", example = "2026-05-15")
    private LocalDate date;

    @Schema(description = "Supplier COGS in TZS: Σ(product.buying_price × quantity_delivered).")
    private BigDecimal revenueTzs;

    @Schema(description = "Human-readable revenue label for cell display.", example = "TZS 1,250,000")
    private String revenueLabel;

    @Schema(description = "Settled mass sold on this day in tonnes.")
    private BigDecimal quantitySoldTonnes;

    @Schema(description = "Human-readable tonnes label for tooltips.", example = "12.5 t")
    private String quantitySoldLabel;

    @Schema(description = "Supplier COGS (TZS): Σ(product.buying_price × quantity_delivered). Current buying_price — not frozen at order time.")
    private BigDecimal cogsTzs;

    @Schema(description = "Human-readable COGS label for tooltips.", example = "TZS 1,250,000")
    private String cogsLabel;

    @Schema(description = "Distinct delivered orders on this day.")
    private long orderCount;

    @Schema(description = "Normalized heat intensity in [0, 1] against the month peak day revenue.")
    private double intensity;

    @Schema(description = "Discrete heat band: NONE, LOW, MEDIUM, HIGH, PEAK.")
    private String intensityLevel;
}
