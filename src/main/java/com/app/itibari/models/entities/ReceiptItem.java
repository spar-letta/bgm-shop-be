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
@Table(name = "receipt_items", schema = "itibari")
public class ReceiptItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "quantity")
    private String quantity;

    @Column(name = "tax_code")
    private String taxCode;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "tax_value")
    private String taxValue;

    @Column(name = "price_per_unit")
    private String pricePerUnit;

    @ManyToOne
    @JoinColumn(name = "receipt_id", referencedColumnName = "id")
    private Receipt receipt;

    @Column(name = "product_public_id")
    private String productPublicId;

    @Column(name = "discount_percentage")
    private String discountPercentage;

    @Column(name = "discount_amount")
    private String discountAmount;

    @Column(name = "sales_order_item_id")
    private String salesOrderItemId;
}
