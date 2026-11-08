package com.app.itibari.models.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record AmendOrderItemRequest(List<Item> itemList) {

    @Builder
    public record Item(@NotNull UUID salesOrderItemPublicId,
                       Integer returnedQuantity,
                       UUID reasonCodePublicId) {
    }
}
