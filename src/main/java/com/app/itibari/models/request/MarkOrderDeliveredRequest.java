package com.app.itibari.models.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkOrderDeliveredRequest {

    private Boolean convertCashToCredit;

    @Size(min = 3, max = 500, message = "Conversion reason must be between 3 and 500 characters when provided")
    private String conversionReason;

    public boolean isConvertCashToCredit() {
        return Boolean.TRUE.equals(convertCashToCredit);
    }
}
