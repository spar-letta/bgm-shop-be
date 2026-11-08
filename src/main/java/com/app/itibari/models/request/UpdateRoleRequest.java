package com.app.itibari.models.request;

import com.app.itibari.dataTypes.ActionType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpdateRoleRequest {

    private List<UUID> permissionIds;

    private ActionType actionType;

    public List<UUID> getPermissionIds() {

        return permissionIds != null ? permissionIds : new ArrayList<>();
    }
}
