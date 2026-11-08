package com.app.itibari.models;

import com.app.itibari.models.entities.BaseEntity;
import com.app.itibari.models.entities.CustomerReturn;
import com.app.itibari.models.entities.RejectionsCode;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_return_items", schema = "itibari")
public class CustomerReturnItem extends BaseEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(BaseView.CustomerReturnView.class)
    @Column(name = "product_name")
    private String productName;

    @Column(name = "order_item_public_id")
    private UUID orderItemPublicId;

    @Column(name = "product_public_id")
    private UUID productPublicId;

    @JsonView(BaseView.CustomerReturnView.class)
    @Column(name = "sku")
    private String sku;

    @JsonView(BaseView.CustomerReturnView.class)
    @Column(name = "order_quantity")
    private Integer orderQuantity;

    @JsonView(BaseView.CustomerReturnView.class)
    @Column(name = "delivered_quantity")
    private Integer deliveredQuantity;

    @JsonView(BaseView.CustomerReturnView.class)
    @Column(name = "returned_quantity")
    private Integer returnedQuantity;

    @JsonView(BaseView.CustomerReturnView.class)
    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @JsonView(BaseView.CustomerReturnView.class)
    @ManyToOne
    @JoinColumn(name = "reason_id", referencedColumnName = "id")
    private RejectionsCode rejectionsCode;
}
