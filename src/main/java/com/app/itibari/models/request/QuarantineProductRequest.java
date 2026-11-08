package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class QuarantineProductRequest {

    private UUID inventoryProductPublicId;

    private Integer quantity;

    private UUID reasonPublicId;
}
