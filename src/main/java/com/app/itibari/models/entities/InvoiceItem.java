package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.AmountType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "invoice_items", schema = "itibari")
public class InvoiceItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sales_order_item_public_id")
    private String salesOrderItemId;

    @Column(name = "sales_order_public_id")
    private String salesOrderId;

    @JsonView({BaseView.SalesOrdersView.class})
    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "percentage")
    private BigDecimal percentage = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "amount_type")
    private AmountType amountType;

    @JsonView({BaseView.SalesOrdersView.class})
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    @JsonView({BaseView.SalesOrdersView.class})
    @Column(name = "has_been_applied")
    private boolean hasBeenApplied;

    @Column(name = "item_total")
    private BigDecimal itemTotal = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "discount_public_id")
    private String discountPublicId;

    @Column(name = "product_public_id")
    private String productPublicId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "quantity")
    private Integer quantity;
}
