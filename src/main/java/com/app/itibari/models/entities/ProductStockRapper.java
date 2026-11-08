package com.app.itibari.models.entities;

import java.math.BigDecimal;

public record ProductStockRapper(String warehouseName,
                                 int productCount,
                                 long totalStock,
                                 BigDecimal totalValue
) {
}