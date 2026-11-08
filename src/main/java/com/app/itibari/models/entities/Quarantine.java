package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.QuarantineStatus;
import com.app.itibari.dataTypes.TransferType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "quarantine", schema = "itibari")
public class Quarantine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "main_warehouse_public_id")
    private UUID mainWarehousePublicId;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "main_warehouse_name")
    private String mainWarehouseName;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "main_warehouse_address")
    private String mainWarehouseAddress;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "quarantine_warehouse_public_id")
    private UUID quarantineWarehousePublicId;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "quarantine_warehouse_name")
    private String quarantineWarehouseName;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private QuarantineStatus quarantineStatus;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "approved_by")
    private String approvedBy;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @JsonView(BaseView.QuarantineView.class)
    @Column(name = "transfer_type")
    @Enumerated(EnumType.STRING)
    private TransferType transferType;

    @Where(clause = "deleted = false")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "quarantine", cascade = CascadeType.ALL)
    private List<QuarantineProducts> quarantineProducts = new ArrayList<>();

    //add
    public void addQuarantineProduct(QuarantineProducts quarantineProduct) {

        Optional<QuarantineProducts> existingQuarantineProduct = quarantineProducts.stream()
                .filter(qp -> qp.getInventoryProductPublicId().equals(quarantineProduct.getInventoryProductPublicId()))
                .findFirst();

        if (existingQuarantineProduct.isEmpty()) {
            quarantineProducts.add(quarantineProduct);
        } else {
            QuarantineProducts existingProduct = existingQuarantineProduct.get();
            existingProduct.setQuantity(existingProduct.getQuantity() + quarantineProduct.getQuantity());
            existingProduct.setReasonPublicId(quarantineProduct.getReasonPublicId());
            existingProduct.setReasonDescription(quarantineProduct.getReasonDescription());
        }
    }

    public void removeQuarantineProduct(QuarantineProducts quarantineProduct) {

        Optional<QuarantineProducts> existingQuarantineProduct = quarantineProducts.stream()
                .filter(qp -> qp.getInventoryProductPublicId().equals(quarantineProduct.getInventoryProductPublicId()))
                .findFirst();

        existingQuarantineProduct.ifPresent(quarantineProducts::remove);
    }
}
