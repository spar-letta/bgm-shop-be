package com.app.itibari.repository;

import com.app.itibari.models.entities.Suppliers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Suppliers, Long>, JpaSpecificationExecutor<Suppliers> {

    Optional<Suppliers> findByPublicIdAndDeletedIsFalse(UUID supplierId);

    Page<Suppliers> findByDeletedIsFalse(Pageable pageable);

    Optional<Suppliers> findBySupplierNameIgnoreCaseAndDeletedIsFalse(String supplierName);

}
