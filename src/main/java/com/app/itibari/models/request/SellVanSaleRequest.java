package com.app.itibari.models.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SellVanSaleRequest {
    @NotNull
    private UUID routePublicId;
    @NotEmpty
    private List<VanItemRequest> items = new ArrayList<>();
    private Boolean requestSpecialDiscount;
    @NotNull
    private UUID customerPublicId;


    public Boolean getRequestSpecialDiscount(){
        return requestSpecialDiscount!=null? requestSpecialDiscount: false;
    }
}
