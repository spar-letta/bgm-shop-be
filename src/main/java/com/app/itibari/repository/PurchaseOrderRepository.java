package com.app.itibari.repository;

import com.app.itibari.models.entities.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    Optional<PurchaseOrder> findByPublicIdAndDeletedIsFalse(UUID purchaseOrderPublicId);

    Optional<PurchaseOrder> findByPurchaseOrderNumberAndDeletedIsFalse(String purchaseOrderNumber);

    List<PurchaseOrder> findAllByReplacementPurchaseOrderIsTrue();
}
