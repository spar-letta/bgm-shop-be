package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PromoItemRequest {

    private UUID productPublicId;

    private long quantityReceived;
}
