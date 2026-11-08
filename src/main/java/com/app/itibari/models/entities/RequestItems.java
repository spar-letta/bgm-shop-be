package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "request_items", schema = "itibari")
public class RequestItems extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "product_public_id")
    private UUID productId;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "product_name")
    private String productName;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "quantity")
    private int quantity;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "unit_price")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "discount_request")
    private BigDecimal discountRequest = BigDecimal.ZERO;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "discount_approved_amount")
    private BigDecimal discountApproved = BigDecimal.ZERO;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Transient
    private BigDecimal totalValue = BigDecimal.ZERO;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "customer_public_id")
    private String customerPublicId;

    @JsonView(BaseView.DiscountApprovalView.class)
    @Column(name = "approved")
    private boolean approved = false;

    @Column(name = "product_item_public_id")
    private String productItemPublicId;


    @Transient
    private BigDecimal totalDiscountRequest = BigDecimal.ZERO;


    @Transient
    private BigDecimal totalApprovedDiscount = BigDecimal.ZERO;

    public BigDecimal getTotalValue() {

        return unitPrice.multiply(new BigDecimal(quantity));
    }

    public BigDecimal getTotalDiscountRequest() {
        return discountRequest.multiply(new BigDecimal(quantity));
    }

    public BigDecimal getTotalApprovedDiscount() {
        return discountApproved.multiply(new BigDecimal(quantity));
    }
}
