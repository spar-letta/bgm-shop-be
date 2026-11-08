package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PermissionRequest {

    private List<String> permissions;

    public List<String> getPermissions() {

        return permissions != null ? permissions : new ArrayList<>();
    }
}
