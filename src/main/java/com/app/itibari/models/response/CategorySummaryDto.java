package com.app.itibari.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class CategorySummaryDto {

    @JsonProperty("description")
    private String description;

    @JsonProperty("qty")
    private String qty;

    @JsonProperty("taxcode")
    private String taxCode;

    @JsonProperty("amt")
    private BigDecimal amt;
}
