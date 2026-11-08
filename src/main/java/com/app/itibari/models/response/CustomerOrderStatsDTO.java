package com.app.itibari.models.response;

import java.math.BigDecimal;

public record CustomerOrderStatsDTO(Long customerId,
                                    Long daysSinceLastOrder,
                                    Long totalOrders,
                                    BigDecimal gmv) {
}
