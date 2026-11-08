package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PurchaseOrderReceiveRequest {

    private List<PurchaseOrderItemReceiveRequest> purchaseOrderItems;

    private String deliveryNoteNumber;
}
