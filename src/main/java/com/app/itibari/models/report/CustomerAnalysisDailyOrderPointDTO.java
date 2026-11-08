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
@Schema(description = "Single-day delivered-order count for Customer Analysis orders trend.")
public class CustomerAnalysisDailyOrderPointDTO {

    private LocalDate date;

    @Schema(description = "Distinct delivered orders with supplier lines on this calendar day.")
    private long deliveredOrderCount;
}
