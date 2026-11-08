package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductProcess {

    private String productName;

    private String buyingPrice;

    private String sellingPrice;

    private String taxMapping;

    private String specifyCustomTax;

    private List<String> suppliers;

    private String category;

    private String productClass;

    private String subClass;

    private String purchaseUnitSize;

    private String purchaseUnitName;

    private String sellingUnit;

    private String unitOfMeasurement;

    private String barCode;

    private String weight;

    private String length;

    private String width;

    private String height;
}
