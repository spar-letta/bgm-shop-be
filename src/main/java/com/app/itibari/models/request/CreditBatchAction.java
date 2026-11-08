package com.app.itibari.models.request;

import com.app.itibari.dataTypes.BatchAction;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreditBatchAction {
    private BatchAction batchAction;
    private List<UUID> creditItemPublicIds;
    private BigDecimal amountPaid;
    private String referenceNumber;
}
