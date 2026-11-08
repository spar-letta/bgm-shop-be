package com.app.itibari.models.request;

import com.app.itibari.dataTypes.CashCreditReconAdjustmentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CashCreditReconAdjustmentRequest {

    private CashCreditReconAdjustmentType type;

    private BigDecimal amount;

    private String note;

    private String attachmentUrl;

    private UUID routePublicId;
}
