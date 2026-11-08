package com.app.itibari.models.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProductInfo {
    private UUID publicId;
    private String productName;
    private String sku;
}
