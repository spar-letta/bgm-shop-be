package com.app.itibari.models.request;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TicketActionDto(String comment, TicketActionType ticketActionType, UUID assignedToId) {
    public enum TicketActionType {
        Assign, Request_More_Information, Resolve, Re_Submit, Viewed
    }
}
