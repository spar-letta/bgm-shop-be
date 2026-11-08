package com.app.itibari.models.response;

import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CustomerReturnOrderItem(@JsonView(BaseView.SalesOrderItemsView.class) String customerName,
                                      @JsonView(BaseView.SalesOrderItemsView.class) String phone,
                                      @JsonView(BaseView.SalesOrderItemsView.class) String district,
                                      @JsonView(BaseView.SalesOrderItemsView.class) String ward,
                                      @JsonView(BaseView.SalesOrderItemsView.class) String storeName,
                                      @JsonView(BaseView.SalesOrderItemsView.class) String customerType,
                                      @JsonView(BaseView.SalesOrderItemsView.class) String orderTotal,
                                      @JsonView(BaseView.SalesOrderItemsView.class) String orderDate,
                                      @JsonView(BaseView.SalesOrderItemsView.class) String status,
                                      @JsonView(BaseView.SalesOrderItemsView.class) String deliveryDate,
                                      @JsonView(BaseView.SalesOrderItemsView.class) UUID orderPublicId,
                                      @JsonView(BaseView.SalesOrderItemsView.class) List<SalesOrderItems> salesOrderItemsList) {
}
