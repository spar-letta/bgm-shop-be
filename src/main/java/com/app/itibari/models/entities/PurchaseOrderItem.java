package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "purchase_order_items", schema = "itibari")
@ToString
public class PurchaseOrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "quantity")
    private Integer quantity;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "received_quantity")
    private Integer quantityReceived;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", referencedColumnName = "id")
    private PurchaseOrder purchaseOrder;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Transient
    private BigDecimal totalAmount;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Transient
    private Integer quantityReturned = 0;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Column(name = "tax_amount")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "amount_after_tax")
    @JsonView(BaseView.purchaseOrderSummaryView.class)
    private BigDecimal amountAfterTax = BigDecimal.ZERO;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Transient
    private Boolean isReceived;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Transient
    private BigDecimal calculatedTaxAmount = BigDecimal.ZERO;

    @JsonView(BaseView.purchaseOrderSummaryView.class)
    @Transient
    private BigDecimal amountReceived = BigDecimal.ZERO;

    public BigDecimal getTotalAmount() {

        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(4, BigDecimal.ROUND_HALF_UP);
    }

    public Boolean getIsReceived() {

        return quantity.equals(quantityReceived);
    }

    public Integer getQuantityReturned() {

        return safeValue(quantity) - safeValue(quantityReceived);
    }

    public BigDecimal getCalculatedTaxAmount() {

        if (this.amountAfterTax == null || this.amountAfterTax.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        } else {
            return getTotalAmount().subtract(this.amountAfterTax).setScale(4, BigDecimal.ROUND_HALF_UP);
        }
    }

    private int safeValue(Integer value) {
        return value != null ? value : 0;
    }

    public int getQuantityReceived(){
        return quantityReceived == null ? 0 : quantityReceived;
    }


    public BigDecimal getAmountReceived() {
        return unitPrice.multiply(BigDecimal.valueOf(getQuantityReceived() - quantityReturned)).setScale(4, BigDecimal.ROUND_HALF_UP);
    }
}
