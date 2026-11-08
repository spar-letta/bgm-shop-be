package com.app.itibari.models.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Supplier Portal V2 dashboard summary cards with trend envelopes.")
public class SummaryCardsV2DTO {

    @Schema(description = "COUNT(DISTINCT customers) with at least one DELIVERED supplier line "
            + "(quantity_delivered > 0) in the window. Matches all-products activeCustomers and "
            + "customer-analysis customers for the same date + geography filters.")
    private TrendMetricDTO<Long> activeCustomers;

    private TrendMetricDTO<Long> totalOrders;

    @Schema(description = "Sold mass in tonnes (product unit weight x delivered quantity; weights are stored in tonnes).")
    private TrendMetricDTO<BigDecimal> totalQuantitySoldTonnes;

     /**
      * COGS (TZS): {@code SUM(product.buyingPrice × quantityDelivered)} on delivered order lines attributed to this
      * supplier. Uses current {@code products.buying_price} — not the sell-price snapshot on the order item.
      * Line-level — not {@code SalesOrders.totalAmount} or whole-order totals.
      */
     @Schema(description = "Supplier COGS in TZS: Σ(product.buying_price × quantity_delivered) for delivered lines attributed to this supplier (line-level; not order totalAmount).")
    private TrendMetricDTO<BigDecimal> totalCogsTzs;
}
