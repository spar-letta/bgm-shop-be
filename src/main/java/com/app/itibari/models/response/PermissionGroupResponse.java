package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PermissionGroupResponse {
    private UUID publicId;
    private String name;
    private List<PermissionDTO> permissions;
}
