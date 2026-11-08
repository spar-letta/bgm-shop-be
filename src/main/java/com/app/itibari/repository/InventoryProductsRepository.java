package com.app.itibari.repository;

import com.app.itibari.models.entities.Inventory;
import com.app.itibari.models.entities.InventoryProducts;
import com.app.itibari.models.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryProductsRepository extends JpaRepository<InventoryProducts, Long>, JpaSpecificationExecutor<InventoryProducts> {

    Optional<InventoryProducts> findByInventoryAndProduct(Inventory inventory, Product product);

    List<InventoryProducts> findAllByProductAndDeletedIsFalse(Product product);

    boolean existsByProductAndInventory(Product product, Inventory inventory);

    Optional<InventoryProducts> findByInventoryAndPublicIdAndDeletedIsFalse(Inventory inventory, UUID inventoryProductPublicId);

    Page<InventoryProducts> findAllByInventoryAndDeletedIsFalse(Inventory inventory, Pageable pageable);

    Optional<InventoryProducts> findByPublicIdAndDeletedIsFalse(UUID inventoryProductPublicId);

    Optional<InventoryProducts> findByInventoryAndProductAndDeletedIsFalse(Inventory inventory, Product product);

    Optional<InventoryProducts> findInventoryProductsByProduct(Product p);

    List<InventoryProducts> findByInventoryAndDeletedIsFalse(Inventory inventory);
}
