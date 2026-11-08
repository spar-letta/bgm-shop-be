package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProductItemUpdate {

    private UUID productItemPublicId;

    private long quantity;
}
