package com.app.itibari.models.response;

import lombok.Builder;


public record BestProductBoughtDto(String productName, String sku, Long quantityDelivered) {
}
