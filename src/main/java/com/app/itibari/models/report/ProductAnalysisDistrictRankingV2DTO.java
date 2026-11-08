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
@Schema(description = "Top wards by settled tonnes for one product. Grouped by organization_location, not organization_regions.")
public class ProductAnalysisDistrictRankingV2DTO {

    private List<ProductAnalysisDistrictRankingRowDTO> rows;

    @Schema(description = "Total settled tonnes in the filtered window across all wards (not limited to returned rows).")
    private BigDecimal totalSettledTonnes;
}
