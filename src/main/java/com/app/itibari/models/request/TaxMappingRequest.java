package com.app.itibari.models.request;

import com.app.itibari.dataTypes.TaxType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TaxMappingRequest {

    private BigDecimal taxRate;

    private String taxName;

    private String taxCode;

    private TaxType taxType;
}
