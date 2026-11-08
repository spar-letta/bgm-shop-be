package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkuProcess {

    private String productName;

    private Integer quantity;

    private String skuCode;

    private String price;
}
