package com.app.itibari.dataTypes;

import java.util.Arrays;
import java.util.List;

public enum PurchaseOrderStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    SUBMITTED,
    AMIN_APPROVED,
    ACCOUNTANT_APPROVED,
    DISPATCHED,
    RECEIVED,
    PARTIAL_RECEIVED,
    PAID,
    PARTIAL_PAID,
    AWAITS_PAYMENT,
    AMIN_REJECTED,
    CLOSED,
    COMPLETED,
    PENDING_CANCEL, PENDING_CREDIT_NOTE_APPROVE, REFUND_REQUESTED, REFUND_APPROVED, REFUND_REJECTED;

    public static List<PurchaseOrderStatus> inThePurchaseOrderStatus() {
        return Arrays.asList(PENDING, SUBMITTED, AMIN_APPROVED, ACCOUNTANT_APPROVED, DISPATCHED);
    }
}
