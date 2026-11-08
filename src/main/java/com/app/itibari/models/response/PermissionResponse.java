package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PermissionResponse {
    private UUID rolePublicId;
    private String roleName;
    private List<PermissionGroupResponse> permissionGroups;
}
