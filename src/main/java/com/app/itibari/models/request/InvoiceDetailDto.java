package com.app.itibari.models.request;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceDetailDto {

    @JsonProperty("description")
    private String description;

    @JsonProperty("qty")
    private String qty;

    @JsonProperty("taxcode")
    private String taxCode;

    @JsonProperty("amt")
    private BigDecimal amt;

    // Everything below is kept in the object but NOT sent in JSON
    @JsonIgnore
    private String taxValue;

    @JsonIgnore
    private String pricePerUnit;

    @JsonIgnore
    private String productName;

    @JsonIgnore
    private String productPublicId;

    @JsonIgnore
    private String discountPercentage;

    @JsonIgnore
    private String discountAmount;

    @JsonIgnore
    private String salesOrderItemId;
}
