package com.app.itibari.models.request;

import com.app.itibari.dataTypes.ChurnedCustomerLeadStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCustomerLeadStatusRequest {

    @NotNull
    private ChurnedCustomerLeadStatus leadStatus;
}
