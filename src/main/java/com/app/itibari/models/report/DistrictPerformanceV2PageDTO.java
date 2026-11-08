package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Paged supplier portal district-performance V2 with full-window supplier line revenue total.")
public class DistrictPerformanceV2PageDTO {

    private long totalElements;
    private int pageSize;
    private int totalPages;
    private boolean last;
    private boolean first;
    private int pageNumber;
    private List<DistrictPerformanceV2RowDTO> content;

    @Schema(description = "Total supplier line revenue (portal COGS) in the filtered window across all buckets "
            + "(not limited to the current page). Σ(product.buying_price × quantity_delivered).")
    private BigDecimal totalSupplierLineRevenueTzs;

    public static DistrictPerformanceV2PageDTO from(Page<DistrictPerformanceV2RowDTO> page,
                                                   BigDecimal totalSupplierLineRevenueTzs) {
        DistrictPerformanceV2PageDTO dto = new DistrictPerformanceV2PageDTO();
        dto.setTotalElements(page.getTotalElements());
        dto.setPageSize(page.getSize());
        dto.setTotalPages(page.getTotalPages());
        dto.setLast(page.isLast());
        dto.setFirst(page.isFirst());
        dto.setPageNumber(page.getNumber());
        dto.setContent(page.getContent());
        dto.setTotalSupplierLineRevenueTzs(totalSupplierLineRevenueTzs);
        return dto;
    }
}
