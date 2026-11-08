package com.app.itibari.models.request;

import com.app.itibari.dataTypes.CustomerOutboundCallStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateCustomerCallStatusRequest {

    @NotNull
    private CustomerOutboundCallStatus status;

    private boolean churned;

    private boolean orderAmendment;

    private boolean locationChange;

    private UUID amendmentPublicId;

    private UUID locationApprovalPublicId;
}
