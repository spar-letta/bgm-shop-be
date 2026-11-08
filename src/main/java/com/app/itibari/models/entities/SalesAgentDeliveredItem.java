package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sales_agent_delivered_items", schema = "itibari")
public class SalesAgentDeliveredItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.SalesOrdersView.class})
    @Column(name = "product_id")
    private UUID productId;

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.OrderAmendmentView.class, BaseView.ReconciliationSummaryView.class, BaseView.SalesOrdersView.class})
    @Column(name = "quantity")
    private Integer quantity;  //quantity delivered

    @JsonView({BaseView.SalesOrderItemsView.class, BaseView.OrderAmendmentView.class, BaseView.ReconciliationSummaryView.class, BaseView.SalesOrdersView.class})
    @Column(name = "price_per_unit")
    private BigDecimal pricePerUnit;

    @Column(name = "order_numbers")
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_agent_id")
    @JsonBackReference
    private SalesOrderAgent salesOrderAgent;
}
