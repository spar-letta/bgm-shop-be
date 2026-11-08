package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Customer Analysis V2 activity calendar: one cell per day in the requested month (zero-filled).")
public class CustomerAnalysisActivityCalendarV2DTO {

    @Schema(description = "Calendar month (1–12).")
    private int month;

    @Schema(description = "Calendar year.")
    private int year;

    @Schema(description = "Human-readable month label.", example = "May 2026")
    private String monthLabel;

    @Schema(description = "One cell per calendar day in the month (zero-filled when no customer activity).")
    private List<CustomerAnalysisActivityCalendarDayV2DTO> days;

    @Schema(description = "Month summary cards: peak day (no trend), total orders, and units sold (settled tonnes) vs previous comparable window.")
    private CustomerAnalysisActivityCalendarSummaryV2DTO summary;
}
