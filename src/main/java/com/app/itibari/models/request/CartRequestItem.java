package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CartRequestItem {

    private UUID productPublicId;

    private Integer quantity;
}
