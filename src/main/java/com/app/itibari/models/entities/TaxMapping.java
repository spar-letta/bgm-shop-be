package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.TaxType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "tax_mapping", schema = "itibari")
public class TaxMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.TaxMappingSummaryView.class, BaseView.ProductDetailView.class, BaseView.GoodsReceived.class, BaseView.GoodsReturned.class, BaseView.purchaseOrderSummaryView.class, BaseView.SalesOrdersView.class, BaseView.InventorySummaryView.class})
    @Column(name = "tax_code")
    private String taxCode;

    @JsonView({BaseView.TaxMappingSummaryView.class, BaseView.ProductDetailView.class, BaseView.GoodsReceived.class, BaseView.GoodsReturned.class, BaseView.purchaseOrderSummaryView.class, BaseView.SalesOrdersView.class, BaseView.InventorySummaryView.class})
    @Column(name = "tax_rate")
    private BigDecimal taxRate;

    @JsonView({BaseView.TaxMappingSummaryView.class, BaseView.ProductDetailView.class, BaseView.GoodsReceived.class, BaseView.GoodsReturned.class, BaseView.purchaseOrderSummaryView.class, BaseView.SalesOrdersView.class, BaseView.InventorySummaryView.class})
    @Column(name = "tax_description")
    private String taxName;  // VAT, GST, etc.

    @JsonView({BaseView.TaxMappingSummaryView.class, BaseView.ProductDetailView.class, BaseView.GoodsReceived.class, BaseView.GoodsReturned.class, BaseView.purchaseOrderSummaryView.class, BaseView.SalesOrdersView.class, BaseView.InventorySummaryView.class})
    @Column(name = "tax_type")
    @Enumerated(EnumType.STRING)
    private TaxType taxType;
}
