package com.app.itibari.models.request;

import com.app.itibari.dataTypes.BnplFunnelStage;
import com.app.itibari.dataTypes.BnplLeadStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BnplLeadRequest {

    private UUID customerPublicId;
    private String phoneNumber;
    private BigDecimal loanEligibilityAmount;
    private BnplFunnelStage funnelStage;
    private BnplLeadStatus leadStatus;
    private UUID assignedAgentPublicId;
}