package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "supplier_targets", schema = "itibari")
public class SupplierTarget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "target_value")
    @JsonView({BaseView.SupplierTargetView.class, BaseView.ProductDetailView.class})
    private Long targetValue = 0L;

    @Column(name = "achieved_value")
    @JsonView({BaseView.SupplierTargetView.class, BaseView.ProductDetailView.class})
    private Long achievedValue = 0L;

    @Column(name = "supplier_public_id")
    @JsonView({BaseView.SupplierTargetView.class, BaseView.ProductDetailView.class})
    private UUID supplierPublicId;

    @Column(name = "supplier_name")
    @JsonView({BaseView.SupplierTargetView.class, BaseView.ProductDetailView.class})
    private String supplierName;

    @Column(name = "product_public_id")
    @JsonView({BaseView.SupplierTargetView.class, BaseView.ProductDetailView.class})
    private UUID productPublicId;

    @Column(name = "product_name")
    @JsonView({BaseView.SupplierTargetView.class, BaseView.ProductDetailView.class})
    private String productName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    private Product product;
}
