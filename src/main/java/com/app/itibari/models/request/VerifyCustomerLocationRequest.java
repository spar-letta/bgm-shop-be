package com.app.itibari.models.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyCustomerLocationRequest {

    @NotNull(message = "Latitude cannot be null")
    private String latitude;

    @NotNull(message = "Longitude cannot be null")
    private String longitude;
}
