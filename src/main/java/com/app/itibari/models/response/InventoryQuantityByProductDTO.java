package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryQuantityByProductDTO {
    private String code;
    private String product;
    private Long quantity = 0L;
}

