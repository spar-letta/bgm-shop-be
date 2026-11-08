package com.app.itibari.models.request;

import lombok.Builder;

@Builder
public record ProductStockDto(Long id,
                              Long warehouseId,
                              String productName,
                              String productSku,
                              Long availableStock,
                              Long reservedStock,
                              Long quarantineStock,
                              Long totalStock,
                              Long closingStock) {
}
