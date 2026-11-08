package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.PromoType;
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
import jakarta.persistence.Transient;
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
@Table(name = "product_items", schema = "itibari")
public class ProductItem extends BaseEntity {

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

    @JsonView({BaseView.GoodsReceived.class, BaseView.VanSalesView.class, BaseView.GoodsReturned.class, BaseView.ProductReconView.class})
    @Column(name = "product_public_id")
    private UUID productPublicId;

    @JsonView({BaseView.GoodsReturned.class, BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class})
    @Column(name = "quantity_returned")
    private long quantityReturned = 0L;

    @JsonView({BaseView.GoodsReceived.class, BaseView.GoodsReturned.class})
    @Column(name = "quantity_received")
    private long quantityReceived = 0L;

    @JsonView({BaseView.GoodsReceived.class, BaseView.GoodsReturned.class})
    @Column(name = "quantity_requested")
    private long quantityRequested = 0L;

    @JsonView(BaseView.GoodsReturned.class)
    @Column(name = "rejection_code")
    private String rejectionCode;

    @JsonView({BaseView.GoodsReturned.class, BaseView.VanSalesView.class})
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "sales_item_public_id")
    private String salesOrderItemPublicId;

    @JsonView({BaseView.ProductReconView.class, BaseView.ReturnView.class, BaseView.VanSalesView.class,
            BaseView.ReconciliationSummaryView.class})
    @Column(name = "quantity_picked")
    private long quantityPicked = 0L;

    @JsonView({BaseView.ProductReconView.class, BaseView.VanSalesView.class, BaseView.ReconciliationSummaryView.class})
    @Column(name = "quantity_sold")
    private long quantitySold = 0L;

    @JsonView({BaseView.ProductReconView.class, BaseView.VanSalesView.class, BaseView.ReconciliationSummaryView.class})
    @Column(name = "quantity_missing")
    private long quantityMissing = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_recon_id", referencedColumnName = "id")
    private ProductRecon productRecon;

    @Column(name = "promo_type")
    @Enumerated(EnumType.STRING)
    private PromoType promoType;

    @JsonView({BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class})
    @Column(name = "total_units")
    private long totalUnits = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "van_sales_id", referencedColumnName = "id")
    private VanSales vanSales;

    @Column(name = "van_sale_quantity")
    @JsonView(BaseView.VanSalesView.class)  //  total_units, van_sale_quantity, order_quantity
    private long vanSaleQuantity = 0L;

    @JsonView(BaseView.VanSalesView.class)
    @Column(name = "order_quantity")
    private long orderQuantity = 0L;

    @JsonView(BaseView.VanSalesView.class)
    @Column(name = "van_sale_item_loaded")
    private boolean loaded = false;

    @JsonView({BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class})
    @Column(name = "is_van_sale_item")
    private boolean isVanSaleItem = false;

    @JsonView({BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class})
    @Column(name = "has_ordered")
    private boolean hasOrdered = false;

    @JsonView({BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class})
    @Column(name = "has_van_sale")
    private boolean hasVanSale = false;

    @JsonView({BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class})
    @Column(name = "initial_van_sale_quantity")
    private Long initialVanSaleQuantity = 0L;

    @Column(name = "product_recon_public_id")
    private String productReconPublicId;

    @JsonView({BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class})
    @Column(name = "van_sale_item_sold")
    private Integer vanSaleItemSold = 0;

    @Transient
    @JsonView({BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class})
    private BigDecimal totalAmountPicked = BigDecimal.ZERO;

    @Transient
    @JsonView({BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class})
    private BigDecimal totalAmountSold = BigDecimal.ZERO;

    @Transient
    @JsonView({BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class})
    private BigDecimal totalAmountMissing = BigDecimal.ZERO;

    @Transient
    @JsonView({BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class, BaseView.GoodsReturned.class, BaseView.GoodsReceived.class})
    private BigDecimal totalAmountReceived = BigDecimal.ZERO;

    @Column(name = "return_note_public_id")
    private String returnNotePublicId;

    @JsonView({BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class})
    @Column(name = "discount")
    private BigDecimal discount = BigDecimal.ZERO;

    @JsonView({BaseView.ProductReconView.class, BaseView.ReconciliationSummaryView.class, BaseView.VanSalesView.class})
    @Column(name = "confirmed_discount")
    private BigDecimal confirmedDiscount = BigDecimal.ZERO;

    public BigDecimal getTotalAmountPicked() {

        BigDecimal amount = new BigDecimal("0.00");

        if (productSellingPrice != null && quantityPicked > 0) {
            amount = productSellingPrice.multiply(BigDecimal.valueOf(quantityPicked));
        }

        return amount;
    }

    public BigDecimal getTotalAmountSold() {

        BigDecimal amount = new BigDecimal("0.00");

        if (productSellingPrice != null && quantitySold > 0) {
            amount = productSellingPrice.multiply(BigDecimal.valueOf(quantitySold));
        }

        return amount;
    }

    public BigDecimal getTotalAmountMissing() {

        BigDecimal amount = new BigDecimal("0.00");

        if (productSellingPrice != null && quantityMissing > 0) {
            amount = productSellingPrice.multiply(BigDecimal.valueOf(quantityMissing));
        }

        return amount;
    }

    public BigDecimal getTotalAmountReceived() {

        BigDecimal amount = new BigDecimal("0.00");

        if (productSellingPrice != null && quantityReceived > 0) {
            amount = productSellingPrice.multiply(BigDecimal.valueOf(quantityReceived));
        }

        return amount;
    }
}
