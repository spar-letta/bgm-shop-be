package com.app.itibari.models.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record TaxCodeRequest(
        @NotEmpty String codeName,

        @Min(0)
        @Max(100)
        Integer codeValue
) {}
