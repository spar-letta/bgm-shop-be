package com.app.itibari.models.entities;

import lombok.Builder;

@Builder
public record SalesSummaryDto(String customer, int orderSize, double averageTotalAmount) {
}
