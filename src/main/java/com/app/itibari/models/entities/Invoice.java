package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.AmountType;
import com.app.itibari.dataTypes.DiscountType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "invoice", schema = "itibari")
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sales_order_public_id")
    private String salesOrderPublicId;

    @JsonView({BaseView.SalesOrdersView.class})
    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "percentage")
    private BigDecimal percentage = BigDecimal.ZERO;

    @Column(name = "amount_type")
    @Enumerated(EnumType.STRING)
    private AmountType amountType;

    @JsonView({BaseView.SalesOrdersView.class})
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    @JsonView({BaseView.SalesOrdersView.class})
    @Column(name = "has_been_applied")
    private boolean hasBeenApplied = false;

    @Column(name = "sales_order_total")
    private BigDecimal salesOrderTotal = BigDecimal.ZERO;

    @JsonView({BaseView.SalesOrdersView.class})
    @Column(name = "discount_type")
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @OneToOne
    @JoinColumn(name = "sales_order_id", referencedColumnName = "id")
    private SalesOrders salesOrder;

    @Column(name = "discount_public_id")
    private String discountPublicId;

    @Transient
    private BigDecimal deductibleAmount = BigDecimal.ZERO;

    @JsonView({BaseView.SalesOrdersView.class})
    @OneToMany(mappedBy = "invoice")
    private List<InvoiceItem> invoiceItems = new ArrayList<>();

    public void addInvoiceItem(InvoiceItem item) {

        Optional<InvoiceItem> existingItem = invoiceItems
                .stream()
                .filter(i -> i.getSalesOrderItemId().equals(item.getSalesOrderItemId()))
                .findFirst();
        if (existingItem.isEmpty()) {
            invoiceItems.add(item);
            item.setInvoice(this);
        }
    }

    public void removeInvoiceItem(InvoiceItem item) {

        invoiceItems.remove(item);
        item.setInvoice(null);
    }


    public BigDecimal getDeductibleAmount() {
        if (!hasBeenApplied) {
            return BigDecimal.ZERO;
        }

        if (invoiceItems != null && !invoiceItems.isEmpty()) {
            return invoiceItems.stream()
                    .filter(item -> Boolean.FALSE.equals(item.getDeleted()))
                    .filter(InvoiceItem::isHasBeenApplied)
                    .map(InvoiceItem::getDiscountAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        if (discountType == DiscountType.SpecialDiscount && discountAmount != null) {
            return discountAmount;
        }

        return BigDecimal.ZERO;
    }

}
