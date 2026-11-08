package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Single calendar day in Customer Analysis activity heat map.")
public class CustomerAnalysisActivityCalendarDayV2DTO {

    @Schema(description = "Calendar date (ISO-8601).", example = "2026-05-15")
    private LocalDate date;

    @Schema(description = "Distinct customers with at least one delivered supplier line on this day.")
    private long distinctCustomerCount;

    @Schema(description = "Distinct delivered orders on this day.")
    private long deliveredOrderCount;

    @Schema(description = "Normalized heat intensity in [0, 1] against the month peak distinct-customer count.")
    private double intensity;

    @Schema(description = "Discrete heat band: NONE, LOW, MEDIUM, HIGH, PEAK.")
    private String intensityLevel;
}
