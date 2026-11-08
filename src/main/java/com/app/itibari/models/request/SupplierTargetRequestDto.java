package com.app.itibari.models.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record SupplierTargetRequestDto(@NotNull UUID supplierPublicId, Long targetValue) {
    public SupplierTargetRequestDto {
        if (targetValue == null) {
            targetValue = 0L;
        }
    }
}

