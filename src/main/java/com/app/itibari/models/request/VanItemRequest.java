package com.app.itibari.models.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class VanItemRequest {
    private UUID productItemPublicId;
    private int quantitySold;
    private BigDecimal discountPerUnit;



    public BigDecimal  getDiscountPerUnit(){
        return discountPerUnit!=null? discountPerUnit: new BigDecimal("0.00");
    }
}
