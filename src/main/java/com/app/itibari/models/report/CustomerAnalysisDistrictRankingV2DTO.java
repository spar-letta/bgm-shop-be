package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Top wards by distinct active customer count for Customer Analysis V2.")
public class CustomerAnalysisDistrictRankingV2DTO {

    private List<CustomerAnalysisDistrictRankingRowV2DTO> rows;

    @Schema(description = "Full-window portal AOV (COGS ÷ delivered orders) for the filtered scope. "
            + "Independent of row limit; not the sum of row-level AOV values.")
    private BigDecimal totalAverageOrderValueTzs;
}
