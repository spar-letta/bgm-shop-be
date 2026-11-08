package com.app.itibari.models.request;

import lombok.Builder;

@Builder
public record PayloadCarrierDto(String purchaseOrderNumber, String shipmentReceivedNotePublicId, String itemReceivedPublicId) {
}
