package com.app.itibari.models.response;

import java.math.BigDecimal;

public record OrderStatsDTO(long totalOrders, BigDecimal totalOrderValue, long totalQuantityDelivered) {
}
