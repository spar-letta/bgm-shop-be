package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "product_stocks", schema = "itibari")
public class ProductStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product_public_id")
    @JsonView(BaseView.ProductStockView.class)
    private UUID productPublicId;

    @Column(name = "inventory_public_id")
    @JsonView(BaseView.ProductStockView.class)
    private UUID inventoryPublicId;

    @Column(name = "product_name")
    @JsonView(BaseView.ProductStockView.class)
    private String productName;

    @Column(name = "product_sku")
    @JsonView(BaseView.ProductStockView.class)
    private String productSku;

    @Column(name = "status")
    private String status;

    @Column(name = "available_stock")
    @JsonView(BaseView.ProductStockView.class)
    private Long availableStock = 0L;

    @Column(name = "reserved_stock")
    @JsonView(BaseView.ProductStockView.class)
    private Long reservedStock = 0L;

    @Column(name = "on_hold_stock")
    @JsonView(BaseView.ProductStockView.class)
    private Long onHoldStock = 0L;

    @Column(name = "quarantined_stock")
    @JsonView(BaseView.ProductStockView.class)
    private Long quarantinedStock = 0L;

    @Column(name = "total_stock")
    @JsonView(BaseView.ProductStockView.class)
    private Long totalStock = 0L;

    @Column(name = "closing_stock")
    @JsonView(BaseView.ProductStockView.class)
    private Long closingStock = 0L;

    @Column(name = "record_today")
    @JsonView(BaseView.ProductStockView.class)
    private LocalDate recordToday;

    @Column(name = "sell_price")
    @JsonView(BaseView.ProductStockView.class)
    private BigDecimal sellPrice = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "product_fk", referencedColumnName = "id")
    private Product product;

    @OneToOne
    @JsonView(BaseView.ProductStockView.class)
    @JoinColumn(name = "warehouse_fk", referencedColumnName = "id")
    private Warehouse warehouse;

    @Column(name = "opening_stock")
    @JsonView(BaseView.ProductStockView.class)
    private Long openingStock = 0L;

    @Transient
    @JsonView(BaseView.ProductStockView.class)
    public BigDecimal getTotalValue() {
        return totalValue();
    }

    @Transient
    @JsonView(BaseView.ProductStockView.class)
    public Long getTotalStockValue() {
        return stockSize();
    }

    public Long stockSize() {
        return Long.sum(availableStock, reservedStock);
    }

    public BigDecimal totalValue() {
        if (totalStock == null || sellPrice == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalStock).multiply(sellPrice);
    }

    public Long getClosingStock() {
        return (safeValue(availableStock)
                + safeValue(reservedStock)
                + safeValue(quarantinedStock));
    }

    private Long safeValue(Long value) {
        return value != null ? value : 0;
    }

    @JsonView(BaseView.ProductStockView.class)
    private boolean getStatus(){
        return product != null && !product.isInActive();
    }

    @JsonView(BaseView.ProductStockView.class)
    public String getProductName() {
        return product != null ? product.getProductName() : null;
    }
}
