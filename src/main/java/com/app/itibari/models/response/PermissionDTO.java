package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PermissionDTO {
    private UUID publicId;
    private String name;
}
