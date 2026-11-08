package com.app.itibari.models.response;

import com.app.itibari.models.entities.ReceivedProductItem;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ReceivedProductItemResponse(@JsonView(BaseView.GoodsReceived.class) BigDecimal grandTotalReceivedValue,
                                          @JsonView(BaseView.GoodsReceived.class) BigDecimal grandTotalValue,
                                          @JsonView(BaseView.GoodsReceived.class) List<ReceivedProductItem> productItemList) {
}
