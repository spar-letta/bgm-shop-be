package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "inventory", schema = "itibari")
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.InventoryView.class)
    @OneToOne
    @JoinColumn(name = "warehouse_fk", referencedColumnName = "id")
    private Warehouse warehouse;

    @JsonIgnore
    @OneToMany(mappedBy = "inventory", fetch = FetchType.LAZY)
    private List<InventoryProducts> inventoryProducts = new ArrayList<>();

    @JsonView(BaseView.InventoryView.class)
    @Column(name = "is_main_inventory")
    private boolean mainInventory;

    @JsonView(BaseView.InventoryView.class)
    @Transient
    private Long totalProducts;

    @JsonView(BaseView.InventoryView.class)
    @Transient
    private Long totalQuantity;

    @JsonView(BaseView.InventoryView.class)
    @Transient
    private BigDecimal totalValue;

    @JsonView(BaseView.InventoryView.class)
    @Transient
    private Long totalAvailableProducts;

    public Long getTotalProducts() {

        return (long) inventoryProducts.size();
    }

    public Long getTotalQuantity() {

        return inventoryProducts.stream().mapToLong(InventoryProducts::getTotalStock).sum();
    }

    public BigDecimal getTotalValue() {

        if (inventoryProducts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalValue = BigDecimal.ZERO;

        for (InventoryProducts inventoryProduct : inventoryProducts) {
            totalValue = totalValue.add(inventoryProduct.getStockValue());
        }
        return totalValue;
    }

    public Long getTotalAvailableProducts() {

        return inventoryProducts
                .stream()
                .filter(inventoryProduct -> inventoryProduct.getAvailableStock() > 0)
                .count();
    }
}
