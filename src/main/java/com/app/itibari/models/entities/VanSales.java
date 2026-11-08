package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.VanSaleStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "van_sales", schema = "itibari")
public class VanSales extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.VanSalesView.class)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private VanSaleStatus status;

    @OneToMany(mappedBy = "vanSales", cascade = CascadeType.ALL)
    @JsonView(BaseView.VanSalesView.class)
    private List<ProductItem> productItems = new ArrayList<>();

    @JsonView(BaseView.VanSalesView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "van_sale_delivery_agent_fk", referencedColumnName = "id")
    private DeliveryAgents deliveryAgents;

    @JsonView(BaseView.VanSalesView.class)
    @Column(name = "route_public_id")
    private String routePublicId;

    @JsonView(BaseView.VanSalesView.class)
    @Transient
    private BigDecimal totalVanSalesValue = BigDecimal.ZERO;

    //add item

    public void addItem(ProductItem item) {

        Optional<ProductItem> optionalProductItem = productItems.stream()
                .filter(existingItem -> existingItem.getPublicId().equals(item.getPublicId()))
                .findFirst();

        if (optionalProductItem.isEmpty()) {
            productItems.add(item);
        }
    }

    public void removeItem(ProductItem item) {

        productItems.removeIf(existingItem -> existingItem.getPublicId()
                .equals(item.getPublicId()));
    }

    public BigDecimal getTotalVanSalesValue() {

        BigDecimal totalValue = BigDecimal.ZERO;

        if (!productItems.isEmpty()) {
            for (ProductItem item : productItems) {
                BigDecimal itemValue = item.getProductSellingPrice()
                        .multiply(BigDecimal.valueOf(item.getInitialVanSaleQuantity() != null ? item.getInitialVanSaleQuantity() : item.getVanSaleQuantity()));

                totalValue = totalValue.add(itemValue);
            }
        }
        return totalValue;
    }
}
