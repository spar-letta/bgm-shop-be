package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryDashboardSummaryDTO {
    private Long totalQuantity = 0L;
    private Long products = 0L;
    private Long regions = 0L;
}

