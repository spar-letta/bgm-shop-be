package com.app.itibari.models.response;

import com.app.itibari.dataTypes.CashCreditReconAdjustmentType;
import com.app.itibari.dataTypes.CashCreditReconAdjustmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CashCreditReconAdjustmentDTO {
    private UUID publicId;
    private CashCreditReconAdjustmentType type;
    private BigDecimal amount;
    private String note;
    private CashCreditReconAdjustmentStatus approvalStatus;
    private String reviewComment;
    private String attachmentUrl;
    private LocalDateTime creationTime;
}
