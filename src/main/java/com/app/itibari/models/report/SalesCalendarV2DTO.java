package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "Supplier portal V2 sales calendar heat map payload.")
public class SalesCalendarV2DTO {

    @Schema(description = "Selected calendar month (1–12).", example = "5")
    private int month;

    @Schema(description = "Selected calendar year.", example = "2026")
    private int year;

    @Schema(description = "Display label for the selected month.", example = "May 2026")
    private String monthLabel;

    private SalesCalendarSummaryV2DTO summary;

    @Schema(description = "One entry per calendar day in the selected month, zero-filled where no deliveries occurred.")
    private List<SalesCalendarDayV2DTO> days;
}
