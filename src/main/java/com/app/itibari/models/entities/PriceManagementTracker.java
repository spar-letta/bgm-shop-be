package com.app.itibari.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "price_management_trackers", schema = "itibari")
public class PriceManagementTracker extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sku")
    private String productSku;

    @Column(name = "previous_purchase_price")
    private BigDecimal previousPurchasePrice = BigDecimal.ZERO;

    @Column(name = "current_purchase_price")
    private BigDecimal currentPurchasePrice = BigDecimal.ZERO;

    @Column(name = "previous_selling_price")
    private BigDecimal previousSellingPrice = BigDecimal.ZERO;

    @Column(name = "current_selling_price")
    private BigDecimal currentSellingPrice = BigDecimal.ZERO;
}
