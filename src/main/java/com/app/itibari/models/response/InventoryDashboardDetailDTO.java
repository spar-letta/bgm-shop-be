package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryDashboardDetailDTO {
    private String code;
    private String product;
    private String region;
    private Long quantity = 0L;
}

