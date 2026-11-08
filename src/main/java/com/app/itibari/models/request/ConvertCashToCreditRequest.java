package com.app.itibari.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ConvertCashToCreditRequest {

    @NotBlank(message = "Conversion reason is required")
    @Size(min = 3, max = 500, message = "Conversion reason must be between 3 and 500 characters")
    private String conversionReason;

    /**
     * Optional. When omitted, the order's stored route or the delivery agent's matching route is used.
     */
    private UUID routePublicId;
}
