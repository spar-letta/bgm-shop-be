package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@ToString
@Table(name = "returned_product_items", schema = "itibari")
public class ReturnedProductItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.GoodsReceived.class, BaseView.ReturnView.class, BaseView.VanSalesView.class, BaseView.GoodsReturned.class,
            BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class})
    @Column(name = "product_name")
    private String productName;

    @JsonView({BaseView.GoodsReceived.class, BaseView.ReturnView.class, BaseView.VanSalesView.class, BaseView.GoodsReturned.class,
            BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class})
    @Column(name = "product_code")
    private String productCode;

    @JsonView({BaseView.ProductReconView.class, BaseView.ReturnView.class, BaseView.VanSalesView.class,
            BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class, BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "price")
    private BigDecimal productSellingPrice = BigDecimal.ZERO;

    @JsonView({BaseView.ProductReconView.class, BaseView.ReturnView.class, BaseView.VanSalesView.class,
            BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class, BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "cost_price")
    private BigDecimal costPrice = BigDecimal.ZERO;

    @JsonView({BaseView.ProductReconView.class, BaseView.ReturnView.class, BaseView.VanSalesView.class,
            BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class, BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Column(name = "total_value")
    private BigDecimal totalValue = BigDecimal.ZERO;

    @JsonView({BaseView.GoodsReceived.class, BaseView.VanSalesView.class, BaseView.GoodsReturned.class, BaseView.ProductReconView.class})
    @Column(name = "product_public_id")
    private UUID productPublicId;

    @JsonView({BaseView.GoodsReturned.class, BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class})
    @Column(name = "quantity_returned")
    private long quantityReturned = 0L;

    @JsonView(BaseView.GoodsReturned.class)
    @Column(name = "rejection_code")
    private String rejectionCode;

    @Column(name = "sales_item_public_id")
    private String salesOrderItemPublicId;

    @ManyToOne
    @JoinColumn(name = "goods_return_delivery_note_fk", referencedColumnName = "id")
    private GoodsReturnDeliveryNote goodsReturnDeliveryNote;

    @JsonView({BaseView.GoodsReceived.class, BaseView.GoodsReturned.class})
    @Column(name = "quantity_requested")
    private long quantityRequested = 0L;
}
