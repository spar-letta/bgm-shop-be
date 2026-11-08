package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreditBatchRequest {
    private UUID parentCustomerPublicId;
    private List<UUID> creditItemPublicIds;
}
