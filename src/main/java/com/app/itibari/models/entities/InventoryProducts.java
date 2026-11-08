package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.ProductStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "inventory_products", schema = "itibari")
public class InventoryProducts extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.InventorySummaryView.class)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_fk", referencedColumnName = "id")
    private Product product;

    @JsonView(BaseView.InventorySummaryView.class)
    @Transient
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_fk", referencedColumnName = "id")
    private Inventory inventory;

    @JsonView(BaseView.InventorySummaryView.class)
    @Column(name = "available_stock")
    private Long availableStock = 0L;

    @JsonView(BaseView.InventorySummaryView.class)
    @Column(name = "reserved_stock")
    private Long reservedStock = 0L;

    @JsonView(BaseView.InventorySummaryView.class)
    @Column(name = "on_hold_stock")
    private Long onHoldStock = 0L;

    @JsonView(BaseView.InventorySummaryView.class)
    @Column(name = "quarantined_stock")
    private Long quarantinedStock = 0L;

    @JsonView(BaseView.InventorySummaryView.class)
    @Transient
    private Long totalStock = 0L;

    @JsonView(BaseView.InventorySummaryView.class)
    @Transient
    private BigDecimal stockValue = BigDecimal.ZERO;

    @JsonView(BaseView.InventorySummaryView.class)
    @Column(name = "closing_stock")
    private Long closingStock = 0L;

    public void getStatus() {

        if (this.availableStock > 10) {
            this.status = ProductStatus.InStock;
        } else {
            this.status = ProductStatus.OutOfStock;
        }
    }

    public Long getTotalStock() {

        return this.totalStock = this.availableStock + this.reservedStock + this.onHoldStock + this.quarantinedStock;
    }

    public BigDecimal getStockValue() {

        if (this.product != null && this.product.getBuyingPrice() != null) {
            return this.stockValue = this.product.getBuyingPrice().multiply(new BigDecimal(this.getAvailableStock()));
        }
        return this.stockValue = BigDecimal.ZERO;
    }
}
