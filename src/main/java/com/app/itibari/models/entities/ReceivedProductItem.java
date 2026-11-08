package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.PromoType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
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
@Table(name = "received_product_items", schema = "itibari")
public class ReceivedProductItem extends BaseEntity {

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
    @Column(name = "purchase_price")
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @JsonView({BaseView.GoodsReceived.class, BaseView.VanSalesView.class, BaseView.GoodsReturned.class, BaseView.ProductReconView.class})
    @Column(name = "product_public_id")
    private UUID productPublicId;

    @Deprecated
    @JsonView({BaseView.GoodsReturned.class, BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class})
    @Column(name = "quantity_returned")
    private long quantityReturned = 0L;

    @JsonView({BaseView.GoodsReceived.class, BaseView.GoodsReturned.class})
    @Column(name = "quantity_received")
    private long quantityReceived = 0L;

    @JsonView({BaseView.GoodsReceived.class, BaseView.GoodsReturned.class})
    @Column(name = "quantity_requested")
    private long quantityRequested = 0L;

    @Deprecated
    @Column(name = "rejection_code")
    private String rejectionCode;

    @Deprecated
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Deprecated
    @Column(name = "sales_item_public_id")
    private String salesOrderItemPublicId;

    @Deprecated
    @Column(name = "quantity_picked")
    private long quantityPicked = 0L;

    @Deprecated
    @Column(name = "quantity_sold")
    private long quantitySold = 0L;

    @Deprecated
    @Column(name = "quantity_missing")
    private long quantityMissing = 0L;

    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_recon_id", referencedColumnName = "id")
    private ProductRecon productRecon;

    @Deprecated
    @Column(name = "promo_type")
    @Enumerated(EnumType.STRING)
    private PromoType promoType;

    @Deprecated
    @Column(name = "total_units")
    private long totalUnits = 0L;

    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "van_sales_id", referencedColumnName = "id")
    private VanSales vanSales;

    @Deprecated
    @Column(name = "van_sale_quantity")
    private long vanSaleQuantity = 0L;

    @Deprecated
    @Column(name = "order_quantity")
    private long orderQuantity = 0L;

    @Deprecated
    @Column(name = "van_sale_item_loaded")
    private boolean loaded = false;

    @Deprecated
    @Column(name = "is_van_sale_item")
    private boolean isVanSaleItem = false;

    @Deprecated
    @Column(name = "has_ordered")
    private boolean hasOrdered = false;

    @Deprecated
    @Column(name = "has_van_sale")
    private boolean hasVanSale = false;

    @Deprecated
    @Column(name = "initial_van_sale_quantity")
    private Long initialVanSaleQuantity = 0L;

    @Deprecated
    @Column(name = "product_recon_public_id")
    private String productReconPublicId;

    @Deprecated
    @Column(name = "van_sale_item_sold")
    private Integer vanSaleItemSold = 0;

    @ManyToOne
    @JoinColumn(name = "goods_received_note_id_fk", referencedColumnName = "id")
    private GoodsReceivedNote goodsReceivedNote;

    @Column(name = "return_note_public_id")
    private String returnNotePublicId;

    @Deprecated
    @Column(name = "discount")
    private BigDecimal discount = BigDecimal.ZERO;

    @JsonView({
            BaseView.ProductReconView.class, BaseView.ReturnView.class, BaseView.VanSalesView.class,
            BaseView.ReconciliationSummaryView.class, BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Transient
    public BigDecimal getTotalValue() {
        BigDecimal price = (purchasePrice != null) ? purchasePrice : BigDecimal.ZERO;
        long qty = (quantityRequested > 0) ? quantityRequested : 0L;
        return price.multiply(BigDecimal.valueOf(qty));
    }

    @JsonView({
            BaseView.ProductReconView.class, BaseView.ReturnView.class, BaseView.VanSalesView.class,
            BaseView.ReconciliationSummaryView.class, BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    @Transient
    public BigDecimal getTotalReceivedValue() {
        BigDecimal price = (purchasePrice != null) ? purchasePrice : BigDecimal.ZERO;
        long qty = (quantityReceived > 0) ? quantityReceived : 0L;
        return price.multiply(BigDecimal.valueOf(qty));
    }
}
