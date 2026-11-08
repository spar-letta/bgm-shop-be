package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class RequestItemDTO {

    private UUID productPublicId;

    private String productName;

    private String sku;

    private long quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalDiscountRequest;  // discount requested per item

    private String productItemPublicId;  // used for vansales discount
}
