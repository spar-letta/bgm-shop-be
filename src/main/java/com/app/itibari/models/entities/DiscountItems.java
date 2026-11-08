package com.app.itibari.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * deprecated class
 */
@Deprecated
@Getter
@Setter
@Entity
@Table(name = "discount_items", schema = "itibari")
public class DiscountItems extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sales_order_public_id")
    private String salesOrderPublicId;

    @Column(name = "sales_order_item_public_id")
    private String salesOrderItemPublicId;

    @Column(name = "product_public_id")
    private String productPublicId;

    @Column(name = "customer_public_id")
    private String customerPublicId;

    @Column(name = "discount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "product_recon_public_id")
    private String productReconPublicId;

    @Column(name = "product_recon_item_public_id")
    private String productReconItemPublicId;
}
