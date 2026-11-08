package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "rejection_reasons", schema = "itibari")
public class RejectionReason extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "valid_reason")
    @JsonView({BaseView.purchaseOrderSummaryView.class, BaseView.SalesOrdersView.class})
    private String validReason;

    @ManyToOne
    @JoinColumn(name = "purchase_order_fk", referencedColumnName = "id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne
    @JoinColumn(name = "sales_orders_fk", referencedColumnName = "id")
    private SalesOrders salesOrders;
}
