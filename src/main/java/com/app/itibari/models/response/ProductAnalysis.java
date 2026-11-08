package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductAnalysis {

    private String productName;

    private String sku;

    private long quantitySold;

    private String region;

    private String location;

    private String locationLat;

    private String locationLng;
}
