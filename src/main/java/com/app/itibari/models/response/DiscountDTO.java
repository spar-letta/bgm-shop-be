package com.app.itibari.models.response;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record DiscountDTO(@JsonView(BaseView.ProductDetailView.class) Integer minVolume,
                          @JsonView(BaseView.ProductDetailView.class) Integer maxVolume,
                          @JsonView(BaseView.ProductDetailView.class) BigDecimal discountedAmount) {
}
