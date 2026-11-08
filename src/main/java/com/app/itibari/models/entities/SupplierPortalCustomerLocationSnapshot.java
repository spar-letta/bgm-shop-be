package com.app.itibari.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "supplier_portal_customer_location_snapshot", schema = "itibari")
public class SupplierPortalCustomerLocationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "snapshot_as_of", nullable = false)
    private LocalDate snapshotAsOf;

    @Column(name = "supplier_public_id", nullable = false)
    private UUID supplierPublicId;

    @Column(name = "customer_public_id", nullable = false)
    private UUID customerPublicId;

    @Column(name = "product_public_id", nullable = false)
    private UUID productPublicId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "latitude")
    private String latitude;

    @Column(name = "longitude")
    private String longitude;

    @Column(name = "customer_active_status")
    private String customerActiveStatus;

    @Column(name = "max_order_time")
    private LocalDateTime maxOrderTime;

    @Column(name = "region_public_id")
    private UUID regionPublicId;

    @Column(name = "region_name")
    private String regionName;

    @Column(name = "location_public_id")
    private UUID locationPublicId;

    @Column(name = "location_name")
    private String locationName;
}
