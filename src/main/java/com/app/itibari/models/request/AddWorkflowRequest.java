package com.app.itibari.models.request;

import lombok.Builder;

import java.util.List;
import java.util.UUID;
@Builder
public record AddWorkflowRequest(List<Transitions> transitionsList) {
    public AddWorkflowRequest {
        transitionsList = transitionsList == null ? List.of() : transitionsList;
    }

    @Builder
    public record Transitions(UUID oldPublicId, UUID from, UUID to, String durationNumber, Integer index) {
    }
}
