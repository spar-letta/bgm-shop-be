package com.app.itibari.models.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record CancelReasonRequest(@NotEmpty String validReason) {
}
