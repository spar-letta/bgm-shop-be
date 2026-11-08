package com.app.itibari.models.request;

import com.app.itibari.dataTypes.ActionType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AddRolesToPersonRequest {

    private ActionType actionType;

    private List<UUID> roleIds;

    public List<UUID> getRoleIds() {

        return roleIds != null ? roleIds : new ArrayList<>();
    }
}
