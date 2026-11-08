package com.app.itibari.models.request;

import com.app.itibari.dataTypes.TicketPriority;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TicketUpdateDto(@NotEmpty String title,
                              @NotEmpty String content,
                              TicketPriority ticketPriority) {
}
