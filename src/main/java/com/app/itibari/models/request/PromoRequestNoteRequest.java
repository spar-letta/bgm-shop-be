package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PromoRequestNoteRequest {

    private List<PromoItemRequest> items;

    private String purchaseOrderNumber;

    private UUID returnNotePublicId;
}
