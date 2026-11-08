package com.app.itibari.models.response;

import com.app.itibari.dataTypes.CashCreditReconLineStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CashCreditReconReportLineDTO {
    private UUID publicId;
    private String routePublicId;
    private String routeId;
    private String deliveryAgentPublicId;
    private String deliveryAgentName;
    private String vehicleNumber;
    private BigDecimal gmvDelivered;
    private BigDecimal discounts;
    private BigDecimal complementaries;
    private BigDecimal netGmv;
    private BigDecimal loadingOffloading;
    private BigDecimal creditOrders;
    private BigDecimal parkingFee;
    private BigDecimal ferryCost;
    private BigDecimal depositedAmount;
    private BigDecimal difference;
    private BigDecimal metaOutstanding;
    private BigDecimal creditDiscounts;
    private CashCreditReconLineStatus status;
    private String sourceReconciliationPublicId;
    private String sourceProductReconPublicId;
    private List<CashCreditReconAdjustmentDTO> adjustments;
}
