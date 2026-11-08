package com.app.itibari.models.response;

import lombok.Builder;

@Builder
public record TicketViewerResponse(String viewedAt,
                                   String department,
                                   String viewedBy) {
}
