package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Optional;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "order_amendment_items", schema = "itibari")
public class AmendmentProductItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Column(name = "product_code")
    private String productCode;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Column(name = "product_name")
    private String productName;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Column(name = "product_quantity")
    private Integer amendmentQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_amendment_id", nullable = false)
    private OrderAmendment orderAmendment;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Column(name = "issued_quantity")
    private Integer issuedQuantity;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Column(name = "delivered_quantity")
    private Integer deliveredQuantity;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Column(name = "price_per_unit")
    private BigDecimal pricePerUnit;

    @JsonView(BaseView.OrderAmendmentView.class)
    @Transient
    public String getCustomerPhone() {
        return Optional.ofNullable(orderAmendment)
                .map(OrderAmendment::getSalesOrder)
                .map(SalesOrders::getCustomer)
                .map(Customers::getPhoneNumber)
                .orElse("--");
    }
}

