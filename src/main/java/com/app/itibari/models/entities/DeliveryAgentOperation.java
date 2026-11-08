package com.app.itibari.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "delivery_agent_operations", schema = "itibari")
public class DeliveryAgentOperation extends BaseEntity{

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

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "total_quantity_picked")
    private int totalQuantityPicked;

    @Column(name = "pre_booked_quantity")
    private int preBookedQuantity;

    @Column(name = "van_booked_quantity")
    private int vanBookedQuantity;

    @Column(name = "pre_booked_sold_quantity")
    private int preBookedSoldQuantity;

    @Column(name = "van_sold_quantity")
    private int vanSoldQuantity;

    @Column(name = "returned_quantity")
    private int preBookedVanQuantity;

    @Column(name = "is_van_sale")
    private boolean vanSale;
}
