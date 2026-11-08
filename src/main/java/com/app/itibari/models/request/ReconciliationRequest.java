package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ReconciliationRequest {

    private UUID publicId;

    private BigDecimal amount;

    private boolean reconciled;
}
