package com.app.itibari.repository;

import com.app.itibari.models.entities.Inventory;
import com.app.itibari.models.entities.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByWarehouseAndDeletedIsFalse(Warehouse warehouse);

    Page<Inventory> findAllByDeletedIsFalse(Pageable pageable);

    Optional<Inventory> findInventoryByPublicIdAndDeletedIsFalse(UUID inventoryPublicId);

    List<Inventory> findAllByDeletedIsFalse();
}
