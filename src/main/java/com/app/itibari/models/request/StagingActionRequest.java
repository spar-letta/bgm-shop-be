package com.app.itibari.models.request;

import lombok.Builder;

@Builder
public record StagingActionRequest(ActionType actionType, String comment, Integer quantity) {
    public enum ActionType{Approve, Reject, Edit_Item}
}
