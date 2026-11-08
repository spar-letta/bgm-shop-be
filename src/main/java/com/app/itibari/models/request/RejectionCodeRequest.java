package com.app.itibari.models.request;

import com.app.itibari.dataTypes.Module;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RejectionCodeRequest {

    private String code;

    private String description;

    private Module module;

    private UUID departmentPublicId;
}
