package com.app.itibari.models.response;

import lombok.Builder;

@Builder
public record HelpDeskTicketSummary(Long totalTickets, Long totalResolved, Long totalPending, Long totalOverdue) {
}
