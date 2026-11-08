package com.app.itibari.models.report;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SalesRevenueDTO {
    private BigDecimal totalRevenue;
    private Integer year;           // Always present
    private Integer month;          // 1-12, null if not monthly
    private Integer quarter;        // 1-4, null if not quarterly
    private String label;
    private String day;
}

//@Chris🤪
// we display
// if its monthly - days of the current month
// if its quarterly - we display based on the current year
// if its yearly - we display months of the current year
