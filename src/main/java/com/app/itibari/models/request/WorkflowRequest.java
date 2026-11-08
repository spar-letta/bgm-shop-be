package com.app.itibari.models.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Builder
public record WorkflowRequest(String name, @NotNull UUID templatePublicId,
                              List<Transitions> transitionsList, UUID assignedDepartmentPublicId) {

    public WorkflowRequest {
        transitionsList = transitionsList == null ? List.of() : transitionsList;
    }

    @Builder
    public record Transitions(UUID from, UUID to, String durationNumber, Integer index) {
        public Transitions{
            if (index == null){
                index = 0;
            }
        }
    }


}
