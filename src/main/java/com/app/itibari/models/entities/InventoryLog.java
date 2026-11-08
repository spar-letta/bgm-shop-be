package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.OpBeforeAfter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "inventory_logs", schema = "itibari")
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "creation_time")
    private LocalDateTime creationTime;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "op_before_after")
    @Enumerated(EnumType.STRING)
    private OpBeforeAfter opBeforeAfter;

    @Column(name = "product_public_id")
    private UUID productPublicId;

    @Column(name = "quantity")
    private Long quantity; // quantity in/out

    @Column(name = "product_available_stock")
    private Long productAvailableStock;

    @Column(name = "product_reserved_stock")
    private Long productReservedStock;

    @Column(name = "inventory_product_available_stock")
    private Long inventoryProductAvailableStock;

    @Column(name = "inventory_product_reserved_stock")
    private Long inventoryProductReservedStock;
}
