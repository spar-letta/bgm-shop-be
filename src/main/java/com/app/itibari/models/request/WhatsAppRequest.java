package com.app.itibari.models.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record WhatsAppRequest(@NotEmpty String customerPhone, @NotEmpty String messageBody) {
}
