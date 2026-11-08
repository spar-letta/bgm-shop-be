package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.DiscountStatus;
import com.app.itibari.dataTypes.DiscountType;
import com.app.itibari.dataTypes.ProductStatus;
import com.app.itibari.models.response.DiscountDTO;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "products", schema = "itibari")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView({BaseView.ProductDetailView.class, BaseView.DiscountApprovalView.class, BaseView.SalesOrderMiniView.class,
            BaseView.SalesOrderItemsView.class,
            BaseView.DiscountView.class,
            BaseView.MiniProdView.class,
            BaseView.OrderAmendmentView.class, BaseView.ReconciliationSummaryView.class, BaseView.GoodsReceived.class, BaseView.GoodsReturned.class, BaseView.purchaseOrderSummaryView.class, BaseView.SalesOrdersView.class, BaseView.InventorySummaryView.class})
    @Column(name = "product_name")
    private String productName;

    @JsonView({BaseView.ProductDetailView.class, BaseView.purchaseOrderSummaryView.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    @JsonView({BaseView.ProductDetailView.class, BaseView.DiscountApprovalView.class,
            BaseView.DiscountView.class, BaseView.SalesOrdersView.class, BaseView.SalesOrderMiniView.class,
            BaseView.InventorySummaryView.class, BaseView.MiniProdView.class, BaseView.purchaseOrderSummaryView.class, BaseView.SalesOrderItemsView.class})
    @Column(name = "sku")
    @Unique
    private String sku;

    @JsonView({BaseView.ProductDetailView.class, BaseView.InventorySummaryView.class})
    @Column(name = "buying_price")
    private BigDecimal buyingPrice = BigDecimal.ZERO;

    @JsonView({BaseView.ProductDetailView.class, BaseView.SalesOrderMiniView.class, BaseView.DiscountView.class, BaseView.SalesOrdersView.class, BaseView.InventorySummaryView.class})
    @Column(name = "sell_price")
    private BigDecimal sellPrice = BigDecimal.ZERO;

    @Column(name = "previous_buy_price")
    @JsonView({BaseView.ProductDetailView.class, BaseView.SalesOrdersView.class, BaseView.InventorySummaryView.class})
    private BigDecimal previousSellPrice = BigDecimal.ZERO;

    @Deprecated
    //@JsonView({BaseView.ProductDetailView.class, BaseView.purchaseOrderSummaryView.class, BaseView.SalesOrdersView.class})
    @Column(name = "quantity")
    private int quantity;

   // @JsonView(BaseView.ProductDetailView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_mapping_id", referencedColumnName = "id")
    private TaxMapping taxMapping;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "custom_tax_addition")
    private BigDecimal customTaxAddition;

    @JsonView(BaseView.ProductDetailView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_class_id", referencedColumnName = "id")
    private ProductClass productClass;

    @JsonView(BaseView.ProductDetailView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_sub_class_id", referencedColumnName = "id")
    private ProductSubClass productSubClass;

   // @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "brand")
    private String brand;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "weight")
    private BigDecimal weight;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "length")
    private Integer length;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "width")
    private Integer width;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "height")
    private Integer height;

    @JsonView({BaseView.ProductDetailView.class, BaseView.purchaseOrderSummaryView.class})
    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus;

    @JsonView({BaseView.ProductDetailView.class, BaseView.purchaseOrderSummaryView.class})
    @Column(name = "barcode")
    private String barcode;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "package_size")
    private String packageSize;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "margin")
    private BigDecimal margin = BigDecimal.ZERO;

    @JsonView(BaseView.ProductDetailView.class)
    @Transient
    private ProductStatus status;

//    @JsonView(BaseView.ProductDetailView.class)

    @Column(name = "available_stock")
    @JsonView({BaseView.ProductDetailView.class, BaseView.SalesOrdersView.class, BaseView.InventorySummaryView.class})
    private Long availableStock = 0L;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "quarantine_stock")
    private Long quarantineStock = 0L;

    @Column(name = "on_hold_stock")
    @JsonView(BaseView.ProductDetailView.class)
    private Long onHoldStock = 0L;

    @Column(name = "reserved_stock")
    @JsonView(BaseView.ProductDetailView.class)
    private Long reservedStock = 0L;

    @Transient
    @JsonView(BaseView.ProductDetailView.class)
    private Long totalStock = 0L;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "bulk_purchase_unit")
    private Integer bulkPurchaseUnit = 1;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "bulk_purchase_unit_name")
    private String bulkPurchaseUnitName;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "selling_unit")
    private Integer sellingUnit;

    @JsonView(BaseView.ProductDetailView.class)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_suppliers", schema = "itibari",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "supplier_id"))
    private List<Suppliers> suppliers = new ArrayList<>();

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "product_best_selling")
    private boolean productBestSelling = false;

    @Column(name = "inactive")
    @JsonView(BaseView.ProductDetailView.class)
    private boolean inActive = false;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "discount_moq")
    private Integer discountMoq;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "description")
    private String description;

    @JsonView(BaseView.ProductDetailView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_code_id", referencedColumnName = "id")
    private TaxCode taxCode;

    @Column(name = "selling_price_before_tax")
    @JsonView(BaseView.ProductDetailView.class)
    private BigDecimal sellingPriceBeforeTax = BigDecimal.ZERO;

    @Column(name = "buying_price_before_tax")
    @JsonView(BaseView.ProductDetailView.class)
    private BigDecimal buyingPriceBeforeTax = BigDecimal.ZERO;

    @Column(name = "tax_amount")
    @JsonView(BaseView.ProductDetailView.class)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "buying_tax_amount")
    @JsonView(BaseView.ProductDetailView.class)
    private BigDecimal buyingTaxAmount = BigDecimal.ZERO;

    @Transient
    @JsonView(BaseView.ProductDetailView.class)
    private Long availableStockItems;

    @Transient
    @JsonView(BaseView.ProductDetailView.class)
    private Long totalStockItems;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonView(BaseView.ProductDetailView.class)
    private List<SupplierTarget> supplierTargets = new ArrayList<>();

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "selling_unit_name")
    private String sellingUnitName;

    @ManyToMany(mappedBy = "productsList")
    private List<Discounts> discounts = new ArrayList<>();

    @JsonView(BaseView.ProductDetailView.class)
    @ManyToOne
    @JoinColumn(name = "product_brand_id", referencedColumnName = "id")
    private ProductBrand productBrand;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "notification")
    private boolean notification;

    @Column(name = "creation_time_ya_notification", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime creationTimeYaNotification;

    @Transient
    @JsonView(BaseView.ProductDetailView.class)
    private String getCreatedByName(){
        return getCreatedBy() != null ? getCreatedBy().getFullName() : "--";
    }

    @Transient
    @JsonView(BaseView.ProductDetailView.class)
    private String getUpdatedByName(){
        return getUpdatedBy() != null ? getUpdatedBy().getFullName() : "--";
    }


    @Transient
    @JsonView(BaseView.ProductDetailView.class)
    private boolean newProduct;

    public Long getTotalStock() {

        return this.availableStock + this.quarantineStock + this.onHoldStock + this.reservedStock;
    }

    public ProductStatus getStatus() {

        if (this.availableStock < 10) {
            return ProductStatus.LowOnStock;
        } else if (this.availableStock > 10) {
            return ProductStatus.InStock;
        } else {
            return ProductStatus.OutOfStock;
        }
    }

    public void addSupplier(Suppliers supplier) {

        Optional<Suppliers> supplierOptional = suppliers.stream()
                .filter(s -> s.getPublicId().equals(supplier.getPublicId()))
                .findFirst();

        if (supplierOptional.isEmpty()) {
            suppliers.add(supplier);
        }
    }

    public void removeSupplier(Suppliers supplier) {

        Optional<Suppliers> supplierOptional = suppliers.stream()
                .filter(s -> s.getPublicId().equals(supplier.getPublicId()))
                .findFirst();

        if (supplierOptional.isPresent()) {
            suppliers.remove(supplier);
        }
    }

    public void applyStock(ProductStock stock) {
        if (stock != null) {
            this.availableStockItems = stock.getAvailableStock();
//            this.availableStock = stock.getAvailableStock();

            this.totalStockItems = stock.getTotalStock();
//            this.totalStock = stock.getTotalStock();
        } else {
            this.availableStockItems = 0L;
//            this.availableStock = 0L;

            this.totalStockItems = 0L;
//            this.totalStock = 0L;
        }
    }

    /**
     * Browser-ready product thumbnail URL (signed GCS / local placeholder).
     * Mirrors POS inventory {@code imageUrl} so list UIs can render {@code <img src>} directly.
     */
    @Transient
    @JsonView({BaseView.ProductDetailView.class, BaseView.ProductSummaryView.class, BaseView.MiniProdView.class})
    private String imageUrl;

    /** Alias retained for POS / Fedha clients that still read {@code productImage}. */
    @Transient
    @JsonView({BaseView.ProductDetailView.class, BaseView.ProductSummaryView.class, BaseView.MiniProdView.class})
    private String productImage;

    @Transient
    @JsonView(BaseView.ProductDetailView.class)
    private List<DiscountDTO> discountUnderProduct = new ArrayList<>();

    public List<DiscountDTO> getDiscountUnderProduct() {
        discountUnderProduct.clear();

        discounts.stream()
                .filter(d -> d.getDiscountType() == DiscountType.VolumeDiscount
                        && d.getStatus() == DiscountStatus.Active
                        && !d.getDeleted())
                .findFirst()
                .ifPresent(activeVolumeDiscount ->
                        activeVolumeDiscount.getVolumeRanges().stream()
                                .sorted(Comparator.comparing(VolumeRange::getMinVolume))
                                .forEach(volumeRange ->
                                        discountUnderProduct.add(
                                                DiscountDTO.builder()
                                                        .minVolume(volumeRange.getMinVolume())
                                                        .maxVolume(volumeRange.getMaxVolume())
                                                        .discountedAmount(volumeRange.getDiscountedAmount())
                                                        .build()
                                        )
                                )
                );

        return discountUnderProduct;
    }

    // new product criteria, created last 3 days


    public boolean isNewProduct() {
        if (creationTimeYaNotification == null) {
            return false;
        }

        if (availableStock < 1){
            return false;
        }

        long daysSinceCreation = Duration.between(creationTimeYaNotification, LocalDateTime.now()).toDays();
        return notification && daysSinceCreation <= 7;
    }
}
