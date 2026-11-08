package com.app.itibari.repository;

import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.PurchaseOrder;
import com.app.itibari.models.entities.PurchaseOrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    Optional<PurchaseOrderItem> findByPublicIdAndDeletedIsFalse(UUID purchaseOrderItemPublicId);

    Optional<PurchaseOrderItem> findByPublicIdAndPurchaseOrderAndDeletedIsFalse(UUID itemPublicId, PurchaseOrder purchaseOrder);

    PurchaseOrderItem findByProductAndPurchaseOrderAndDeletedIsFalse(Product product, PurchaseOrder purchaseOrder);

    List<PurchaseOrderItem> findAllByPurchaseOrderAndDeletedIsFalse(PurchaseOrder purchaseOrder);

    Page<PurchaseOrderItem> findAllByPurchaseOrderAndDeletedIsFalse(PurchaseOrder purchaseOrder, Pageable pageable);
}
