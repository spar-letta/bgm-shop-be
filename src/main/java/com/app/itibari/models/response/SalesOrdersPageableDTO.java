package com.app.itibari.models.response;

import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
public class SalesOrdersPageableDTO {
    @JsonView(BaseView.SalesOrdersView.class) private long totalElements;
    @JsonView(BaseView.SalesOrdersView.class) private long pageSize;
    @JsonView(BaseView.SalesOrdersView.class) private long totalPages;
    @JsonView(BaseView.SalesOrdersView.class) private boolean last;
    @JsonView(BaseView.SalesOrdersView.class) private boolean first;
    @JsonView(BaseView.SalesOrdersView.class) private long pageNumber;
    @JsonView(BaseView.SalesOrdersView.class) private BigDecimal totalOrderValue;
    @JsonView(BaseView.SalesOrdersView.class) private List<SalesOrders> content;
}
