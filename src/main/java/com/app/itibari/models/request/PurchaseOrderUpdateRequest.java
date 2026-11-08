package com.app.itibari.models.request;

import com.app.itibari.dataTypes.PurchaseOrderAction;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PurchaseOrderUpdateRequest {

    private PurchaseOrderAction purchaseOrderAction;

    private List<PurchaseOrderItemRequest> purchaseOrderItems;

    private String orderNote;

    private String cancelReason;

    private UUID reasonCodePublicId;
}
