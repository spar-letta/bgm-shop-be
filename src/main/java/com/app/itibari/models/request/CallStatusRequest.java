package com.app.itibari.models.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallStatusRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String description;
}
