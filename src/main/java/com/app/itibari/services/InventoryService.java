package com.app.itibari.services;

import com.app.itibari.models.entities.Inventory;
import com.app.itibari.models.entities.InventoryProducts;
import com.app.itibari.models.request.ClosingUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InventoryService {

    Inventory getInventoryPerWarehouse(UUID warehousePublicId);

    Page<InventoryProducts> getInventoryProducts(UUID inventoryPublicId, UUID productPublicId, String searchParam, Pageable pageable);

    Page<Inventory> getAllInventories(String searchParam, Pageable pageable);

    Page<InventoryProducts> getInventoryProductsByWarehouse(UUID warehousePublicId, String searchParam, Pageable pageable);

    void updateClosingStock(@Valid ClosingUpdateRequest request, String name);

    Inventory getInventory(UUID inventoryPublicId);
}
