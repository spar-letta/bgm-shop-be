package com.app.itibari.repository;

import com.app.itibari.models.entities.ProductAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProductAdjustmentRepository extends JpaRepository<ProductAdjustment, Long>, JpaSpecificationExecutor<ProductAdjustment> {

    Page<ProductAdjustment> findByProductNameContainingIgnoreCaseAndDeletedIsFalse(String searchParam, Pageable pageable);

    Page<ProductAdjustment> findByDeletedIsFalse(Pageable pageable);

    Optional<ProductAdjustment> findByPublicIdAndDeletedIsFalse(UUID publicId);
}
