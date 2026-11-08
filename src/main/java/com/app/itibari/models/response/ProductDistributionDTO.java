package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductDistributionDTO {

    private String customerName;

    private String customerPublicId;

    private String productName;

    private String sku;

    private long quantity;

    private String region;

    private String location;

    private String city;

    private String locationLat;

    private String locationLng;

    private BigDecimal price;

    private String date;
}
