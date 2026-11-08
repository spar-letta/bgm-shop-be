package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.AdjustmentStatus;
import com.app.itibari.dataTypes.AdjustmentType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "product_adjustment", schema = "itibari")
public class ProductAdjustment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "product_public_id")
    private String productPublicId;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "product_name")
    private String productName;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "product_sku")
    private String productSku;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "quantity")
    private int quantity;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "reason")
    private String reason;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "adjustment_type")
    @Enumerated(EnumType.STRING)
    private AdjustmentType adjustmentType;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AdjustmentStatus status;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "warehouse_public_id")
    private String warehousePublicId;

    @JsonView(BaseView.ProductDetailView.class)
    @Column(name = "warehouse_name")
    private String warehouseName;
}
