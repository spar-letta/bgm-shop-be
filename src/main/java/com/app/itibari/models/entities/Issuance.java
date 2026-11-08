package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.LoadingType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "issuance", schema = "itibari")
public class Issuance extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_agent_public_id")
    private UUID deliveryAgentPublicId;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "sku")
    private String sku;

    @Column(name = "warehouse_public_id")
    private UUID warehousePublicId;

    @Column(name = "pre_booked_quantity")
    private int preBookedQuantity;

    @Column(name = "van_booked_quantity")
    private int vanBookedQuantity;
}
