package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PurchaseOrderItemReceiveRequest {

    private UUID orderItemPublicId;

    private Integer quantityReceived;
}
