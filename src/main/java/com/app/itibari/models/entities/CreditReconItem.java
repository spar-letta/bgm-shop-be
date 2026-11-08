package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "credit_recon_items", schema = "itibari")
public class CreditReconItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_recon_id")
    private CreditRecon creditRecon;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "product_public_id")
    private String productPublicId;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "product_name")
    private String productName;

    @Column(name = "sales_order_item_public_id")
    private String salesOrderItemPublicId;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "quantity")
    private Integer quantity = 0;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "unit_price")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "total_price")
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @ManyToOne
    @JoinColumn(name = "zone_id_fk", referencedColumnName = "id")
    private OrganizationLocations zone;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "reconciled")
    private Boolean reconciled = false;

}
