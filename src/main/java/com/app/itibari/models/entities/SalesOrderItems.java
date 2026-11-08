package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.AmountType;
import com.app.itibari.dataTypes.OrderItemStatus;
import com.app.itibari.dataTypes.OrderType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sales_order_items", schema = "itibari")
@Where(clause = "deleted = false")
public class SalesOrderItems extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class})
    @Column(name = "sales_order_public_id")
    private UUID salesOrderPublicId;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class})
    @Column(name = "product_id")
    private UUID productId;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrderMiniView.class,
            BaseView.ReconciliationSummaryView.class, BaseView.SalesOrdersView.class})
    @Column(name = "quantity")
    private int quantity;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrderMiniView.class, BaseView.ReconciliationSummaryView.class, BaseView.SalesOrdersView.class})
    @Column(name = "price_per_unit")
    private BigDecimal pricePerUnit;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrderMiniView.class, BaseView.ReconciliationSummaryView.class, BaseView.SalesOrdersView.class})
    @Column(name = "total_amount")
    private BigDecimal totalAmount;   // dhould be transient and calculated

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "tax_amount")
    private BigDecimal taxAmount;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.OrderAmendmentView.class, BaseView.SalesOrderMiniView.class, BaseView.ReconciliationSummaryView.class, BaseView.SalesOrdersView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_items_product_fk", referencedColumnName = "id")
    private Product product;

    @JsonView({BaseView.SalesOrderItemsView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id")
    private SalesOrders salesOrder;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class})
    @Column(name = "loaded")
    private boolean loaded = false;  // helps in returning quantity vack id

    @Transient
    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class})
    private BigDecimal weight;

    @Transient
    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class})
    private String dimensions;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "reconciliation_id")
    private String reconciliationId;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderItemStatus status = OrderItemStatus.NotDelivered;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Transient
    private BigDecimal totalPayable = BigDecimal.ZERO;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Transient
    private BigDecimal totalTaxPayable = BigDecimal.ZERO;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Transient
    private BigDecimal totalPayableWithTax = BigDecimal.ZERO;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "quantity_delivered")
    private int quantityDelivered = 0;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "discount")
    private BigDecimal discount = BigDecimal.ZERO;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "has_discount")
    private boolean hasDiscount = false;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "amount_type")
    @Enumerated(EnumType.STRING)
    private AmountType amountType;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    //@JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class})
    @Column(name = "sales_agent_public_id")
    private String salesAgentPublicId;

    @ManyToOne
    @JoinColumn(name = "updated_by_customer", referencedColumnName = "id")
    private Customers updatedByCustomer;

    @ManyToOne
    @JoinColumn(name = "updated_by_sales_agent_fk", referencedColumnName = "id")
    private SalesAgents updatedBySalesAgent;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "delivered_date")
    private LocalDateTime deliveredDate;

    @Column(name = "tax_amount_value")
    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    private BigDecimal taxAmountValue = BigDecimal.ZERO;

    @Column(name = "new_discount_percentage")
    private BigDecimal newDiscountPercentage = BigDecimal.ZERO;

    @Column(name = "product_price_before_tax")
    private BigDecimal productBeforePriceAfterTax = BigDecimal.ZERO;

    @Column(name = "tax_percentage_applied")
    private BigDecimal taxPercentageApplied = BigDecimal.ZERO;

    @Column(name = "is_tax_applied")
    private boolean taxApplied = false;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "walk_in_volume_discount")
    private BigDecimal walkInVolumeDiscount; // qty * discount given

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    @Column(name = "walk_in_special_discount")
    private BigDecimal walkInSpecialDiscount; // qty * discount given

    @Column(name = "van_sale_converted")
    private boolean vanSaleConverted = false;


    @Transient
    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    private BigDecimal totalItemTaxAmount = BigDecimal.ZERO;


    @Transient
    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class})
    private BigDecimal bnplAmount = BigDecimal.ZERO;

    public BigDecimal getTotalPayable() {

        BigDecimal price = pricePerUnit != null ? pricePerUnit.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;

        if (OrderItemStatus.NotDelivered.equals(status)) {
            if (hasDiscount) {
                return price.subtract(discount != null ? discount : BigDecimal.ZERO);
            }
            return price;
        } else if (OrderItemStatus.PartiallyDelivered.equals(status) || OrderItemStatus.Delivered.equals(status)) {
            BigDecimal amount = pricePerUnit != null ? pricePerUnit.multiply(BigDecimal.valueOf(quantityDelivered)) : BigDecimal.ZERO;
            if (hasDiscount) {
                return amount.subtract(discount != null ? discount : BigDecimal.ZERO);
            }
            return amount;
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getTotalPayableForOrderDelivered(BigDecimal discountAmount) {

        if (OrderItemStatus.PartiallyDelivered.equals(status) || OrderItemStatus.Delivered.equals(status)) {
            BigDecimal amount = pricePerUnit != null ? pricePerUnit.multiply(BigDecimal.valueOf(quantityDelivered)) : BigDecimal.ZERO;
            BigDecimal itemDiscountAmount = discountAmount.multiply(BigDecimal.valueOf(quantityDelivered));
            return amount.subtract(itemDiscountAmount);
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getTotalPayableForReceipt() {

        BigDecimal price = Optional.ofNullable(product)
                .map(Product::getSellingPriceBeforeTax)
                .map(sp -> sp.multiply(BigDecimal.valueOf(quantity)))
                .orElse(BigDecimal.ZERO);


        if (OrderItemStatus.NotDelivered.equals(status)) {
            if (hasDiscount) {
                return price.subtract(discount != null ? discount : BigDecimal.ZERO);
            }
            return price;
        } else if (OrderItemStatus.PartiallyDelivered.equals(status) || OrderItemStatus.Delivered.equals(status)) {
//            BigDecimal amount = pricePerUnit != null ? pricePerUnit.multiply(BigDecimal.valueOf(quantityDelivered)) : BigDecimal.ZERO;
            BigDecimal amount = Optional.ofNullable(product)
                    .map(Product::getSellingPriceBeforeTax)
                    .map(sp -> sp.multiply(BigDecimal.valueOf(quantityDelivered)))
                    .orElse(BigDecimal.ZERO);
            if (hasDiscount) {
                return amount.subtract(discount != null ? discount : BigDecimal.ZERO);
            }
            return amount;
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getTotalTaxPayable() {

        BigDecimal price = product.getTaxMapping() != null ? product
                .getTaxMapping()
                .getTaxRate()
                .multiply(getTotalPayable()) : BigDecimal.ZERO;

        if (OrderItemStatus.NotDelivered.equals(status)) {
            return price;
        } else if (OrderItemStatus.PartiallyDelivered.equals(status) || OrderItemStatus.Delivered.equals(status)) {
            return product.getTaxMapping() != null ? product.getTaxMapping().getTaxRate().multiply(getTotalPayable()) : BigDecimal.ZERO;
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getTotalPayableWithTax() {

        return getTotalPayable().add(getTotalTaxPayable());
    }

    public BigDecimal getWeight() {

        return product.getWeight() != null ? product.getWeight().multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
    }

    public String getDimensions() {

        String length = product.getLength() != null ? product.getLength().toString() : "0";
        String width = product.getWidth() != null ? product.getWidth().toString() : "0";
        String height = product.getHeight() != null ? product.getHeight().toString() : "0";

        return length + "cm" + "x" + width + "cm" + "x" + height + "cm";
    }

    public BigDecimal getTotalItemTaxAmount() {
        if (this.product != null && this.product.getTaxAmount() != null) {
            if (this.quantityDelivered > 0) {
                return this.product.getTaxAmount().multiply(BigDecimal.valueOf(this.quantityDelivered));
            } else {
                return this.product.getTaxAmount().multiply(BigDecimal.valueOf(this.quantity));
            }

        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getBnplAmount() {
        BigDecimal amount = BigDecimal.ZERO;
        if (salesOrder.getOrderType() != OrderType.CREDIT) {
            return amount;
        }

        BigDecimal rate = salesOrder.getInterestRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal newRate = rate.add(BigDecimal.valueOf(1));
        amount = getTotalPayable().multiply(newRate);
        return amount;
    }
}
