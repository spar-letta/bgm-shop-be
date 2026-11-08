package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.QuarantineProductStatus;
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
@Table(name = "quarantine_products", schema = "itibari")
public class QuarantineProducts extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "inventory_product_public_id")
    private UUID inventoryProductPublicId;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "inventory_product_name")
    private String productName;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "inventory_product_code")
    private String productCode;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "quantity")
    private Integer quantity;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "reason_public_id")
    private String reasonPublicId;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "reason_description")
    private String reasonDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quarantine_id", referencedColumnName = "id")
    private Quarantine quarantine;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "is_disposed")
    private boolean disposed;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "disposed_quantity")
    private Integer disposedQuantity;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "disposed_status")
    @Enumerated(EnumType.STRING)
    private QuarantineProductStatus disposedStatus;

    @JsonView(BaseView.QuarantineView.class)
    @ManyToOne
    @JoinColumn(name = "disposal_reason_id_fk", referencedColumnName = "id")
    private RejectionsCode disposalReason;

    @Column(name = "disposal_comment")
    @JsonView(BaseView.QuarantineView.class)
    private String disposalComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposal_initiated_by", referencedColumnName = "id")
    private Person disposalInitiatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposal_approved_by", referencedColumnName = "id")
    private Person disposalApprovedBy;

    @Transient
    @JsonView(BaseView.QuarantineView.class)
    public UUID getQuarantinePublicId(){
        if (quarantine != null){
            return quarantine.getPublicId();
        }
        return null;
    }
}
