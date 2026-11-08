package com.app.itibari.models.response;

import com.app.itibari.dataTypes.CashCreditReconReportStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CashCreditReconReportDTO {
    private UUID publicId;
    private LocalDate reportDate;
    private CashCreditReconReportStatus status;
    private LocalDateTime generatedAt;
    private LocalDateTime reconciledAt;
    private String reconciledBy;
    private String reconciliationComment;
    private BigDecimal totalGmvDelivered;
    private BigDecimal totalDiscounts;
    private BigDecimal totalComplementaries;
    private BigDecimal totalNetGmv;
    private BigDecimal totalLoadingOffloading;
    private BigDecimal totalCreditOrders;
    private BigDecimal totalParkingFee;
    private BigDecimal totalFerryCost;
    private BigDecimal totalDepositedAmount;
    private BigDecimal totalDifference;
    private BigDecimal totalMetaOutstanding;
    private BigDecimal totalCreditDiscounts;
    private List<CashCreditReconReportLineDTO> lines;
}
