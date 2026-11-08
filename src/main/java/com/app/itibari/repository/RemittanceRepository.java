package com.app.itibari.repository;

import com.app.itibari.models.entities.Remittances;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface RemittanceRepository extends JpaRepository<Remittances, Long>, JpaSpecificationExecutor<Remittances> {

    Optional<Remittances> findRemittancesByInvoiceNumberIgnoreCaseAndDeletedIsFalse(String invoiceNumber);

    Optional<Remittances> findByPublicIdAndDeletedIsFalse(UUID remittancePublicId);

    Optional<Remittances> findByPurchaseOrderNumberAndDeletedFalse(String purchaseOrderNumber);
}
