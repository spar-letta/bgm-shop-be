package com.app.itibari.models.response;

import com.app.itibari.models.entities.InventoryProducts;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonView(BaseView.InventorySummaryView.class)
public class ProductViewResponse {

    @JsonView(BaseView.InventorySummaryView.class)
    private InventoryProducts product;

    @JsonView(BaseView.InventorySummaryView.class)
    private UUID warehousePublicId;

    @JsonView(BaseView.InventorySummaryView.class)
    private String warehouseName;
}
