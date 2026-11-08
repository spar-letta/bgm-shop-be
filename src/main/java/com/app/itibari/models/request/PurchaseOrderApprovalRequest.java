package com.app.itibari.models.request;

import com.app.itibari.dataTypes.ActionType;
import com.app.itibari.dataTypes.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PurchaseOrderApprovalRequest {

    private String comment;

    private ActionType actionType;

    private String paymentReference;

    private PaymentMethod paymentMethod;

    private BigDecimal amountPaid = BigDecimal.ZERO;

    private List<UUID> creditNotePublicIds = new ArrayList<>();

}
