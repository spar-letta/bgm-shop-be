package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "special_item", schema = "itibari")
public class SpecialItem extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.DiscountView.class)
    @Column(name = "product_public_id")
    private UUID productId;

    @JsonView(BaseView.DiscountView.class)
    @Column(name = "product_name")
    private String productName;

    @JsonView(BaseView.DiscountView.class)
    @Column(name = "amount_approved_per_quantity")
    private BigDecimal amountApprovedPerQuantity = BigDecimal.ZERO;

    @JsonView(BaseView.DiscountView.class)
    @Column(name="quantity_requested")
    private Integer quantityRequested;
}
