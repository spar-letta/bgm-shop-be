package com.app.itibari.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductStockRapperResponse {
    String getWarehouseName();
    UUID getWarehousePublicId();
    Long getProductCount();
    Long getTotalStock();
    BigDecimal getTotalValue();
    BigDecimal getAverageCost();
}
