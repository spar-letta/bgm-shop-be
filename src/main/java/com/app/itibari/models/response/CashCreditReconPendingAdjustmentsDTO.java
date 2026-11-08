package com.app.itibari.models.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Logistics-expense adjustments, including pending and reviewed records, grouped by report line. */
@Getter
@Builder
public class CashCreditReconPendingAdjustmentsDTO {
    private UUID reportLinePublicId;
    private String routePublicId;
    private String routeId;
    private String deliveryAgentPublicId;
    private String deliveryAgentName;
    private LocalDate reportDate;
    private String vehicleNumber;
    private boolean reconciled;
    private String status;
    /**
     * Kept for clients already using this field. It now contains the complete
     * adjustment history, not only pending items.
     */
    private List<CashCreditReconAdjustmentDTO> pendingAdjustments;
    private List<CashCreditReconAdjustmentDTO> adjustments;
}
