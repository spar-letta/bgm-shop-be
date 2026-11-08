package com.app.itibari.models.request;

import com.app.itibari.dataTypes.ActionType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AmendRequest {

    private UUID amendOrderPublicId;

    private ActionType actionType;

    private UUID reasonCodePublicId;

    private String comment = "";
}
