package com.app.itibari.models.request;

import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record AgentAction(@NotNull ActionType actionType, Set<UUID> customerPublicIds,
                          UUID salesAgentToReceiveCustomerPublicId) {
    public enum ActionType{Map_Customer, De_Activate_Agent}
    public AgentAction {
        if (customerPublicIds == null) {
            customerPublicIds = new HashSet<>();
        }
    }
}
