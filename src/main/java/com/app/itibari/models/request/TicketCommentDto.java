package com.app.itibari.models.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record TicketCommentDto(@NotEmpty String content, Boolean isExternal) {
}
