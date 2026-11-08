package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProductSlimDTO {

    private UUID publicId;
    private String productName;
    private BigDecimal sellPrice;
}
