package com.app.itibari.models.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record FaqUpdateRequest(@NotNull UUID issueTypePublicId,
                               @NotEmpty String question,
                               @NotEmpty String answer) {
}
