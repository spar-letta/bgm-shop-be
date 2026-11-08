package com.app.itibari.models.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record ReverseRequest(@NotEmpty String reason, @NotEmpty String department) {
}
