package com.app.itibari.models.response;

import com.app.itibari.dataTypes.BnplCreditLimitAdjustmentType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class BnplCreditLimitAdjustmentResponse {

    private UUID publicId;
    private UUID customerPublicId;
    private String customerName;
    private String customerPhoneNumber;
    private UUID walletPublicId;
    private BnplCreditLimitAdjustmentType adjustmentType;
    private BigDecimal percentage;
    private BigDecimal oldCreditLimit;
    private BigDecimal newCreditLimit;
    private BigDecimal oldCreditLimitBalance;
    private BigDecimal newCreditLimitBalance;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String triggeredByCreditItemIds;
    private String reason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime creationTime;
}
