package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Peak distinct-customer activity day within a Customer Analysis activity-calendar month.")
public class CustomerAnalysisActivityCalendarPeakDayV2DTO {

    @Schema(description = "Calendar date of the peak day (earliest date on ties).")
    private LocalDate date;

    @Schema(description = "Distinct customers with at least one delivered supplier line on the peak day.")
    private long distinctCustomerCount;

    @Schema(description = "Distinct delivered orders on the peak day.")
    private long deliveredOrderCount;
}
