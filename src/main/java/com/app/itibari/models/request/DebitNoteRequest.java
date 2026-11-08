package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class DebitNoteRequest {

    private UUID reconciliationPublicId;

    private UUID deliveryAgentPublicId;

    private String description;

    private BigDecimal amount;
}
