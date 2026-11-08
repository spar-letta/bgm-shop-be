package com.app.itibari.models.request;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record WorkflowStepRequest(List<Transitions> transitionsList) {

    public WorkflowStepRequest {
        transitionsList = transitionsList == null ? List.of() : transitionsList;
    }

    @Builder
    public record Transitions(UUID from, UUID to) {
    }
}
