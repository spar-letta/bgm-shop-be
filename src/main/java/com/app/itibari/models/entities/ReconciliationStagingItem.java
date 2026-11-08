package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "reconciliation_staging_items", schema = "itibari")
public class ReconciliationStagingItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "anticipated_product")
    @JsonView(BaseView.ReturnView.class)
    private Integer anticipatedProduct;

    @Column(name = "quantity_returned")
    @JsonView(BaseView.ReturnView.class)
    private Integer quantityReturned;

    @Column(name = "reason")
    @JsonView(BaseView.ReturnView.class)
    private String reason;

    @Column(name = "product_public_id")
    @JsonView(BaseView.ReturnView.class)
    private UUID productPublicId;

    @Column(name = "product_name")
    @JsonView(BaseView.ReturnView.class)
    private String productName;

    @ManyToOne
    @JoinColumn(name = "reconciliation_staging_id", referencedColumnName = "id")
    private ReconciliationStaging reconciliationStaging;
}
