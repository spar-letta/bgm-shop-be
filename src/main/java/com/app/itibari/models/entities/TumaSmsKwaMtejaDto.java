package com.app.itibari.models.entities;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record TumaSmsKwaMtejaDto(@NotEmpty String messageBody) {
}
