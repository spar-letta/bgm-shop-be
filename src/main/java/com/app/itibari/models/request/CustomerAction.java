package com.app.itibari.models.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CustomerAction(@NotNull ActionType actionType, String rejectionCode) {

    public enum ActionType{
        Activate, Approve_DeActivate, De_Activate
    }
}
