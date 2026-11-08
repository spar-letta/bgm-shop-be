package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.ReconStatus;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "product_recon", schema = "itibari")
public class ProductRecon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.ProductReconView.class)
    @Column(name = "warehouse_name")
    private String warehouseName;

    @JsonView(BaseView.ProductReconView.class)
    @Column(name = "warehouse_public_id")
    private String warehousePublicId;

    @JsonView(BaseView.ProductReconView.class)
    @Column(name = "delivery_id")
    private String deliveryId;

    @JsonView(BaseView.ProductReconView.class)
    @Column(name = "delivery_agent")
    private String deliveryAgent;

    @JsonView(BaseView.ProductReconView.class)
    @Column(name = "delivery_agent_public_id")
    private String deliveryAgentPublicId;

    @JsonView(BaseView.ProductReconView.class)
    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Where(clause = "deleted = false")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "productRecon", cascade = CascadeType.ALL)
    private List<ProductItem> productItems = new ArrayList<>();

    @JsonView(BaseView.ProductReconView.class)
    @Column(name = "reconciled")
    private boolean reconciled = false;

    @JsonView(BaseView.ProductReconView.class)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ReconStatus status;

    @JsonView({BaseView.ProductReconView.class})
    @Column(name = "walk_in")
    private boolean walkIn;

    @JsonView({BaseView.ProductReconView.class})
    @Column(name = "locked")
    private boolean locked = false;

//    private OrganizationLocations zone;

    public void addProductItem(ProductItem savedProductItem) {

        Optional<ProductItem> existingProductItem = productItems.stream()
                .filter(item -> item.getPublicId().equals(savedProductItem.getPublicId()))
                .findFirst();

        if (existingProductItem.isEmpty()) {
            productItems.add(savedProductItem);
        }
    }
}
