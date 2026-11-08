package com.app.itibari.repository;

import com.app.itibari.models.entities.SupplierTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupplierTargetRepository extends JpaRepository<SupplierTarget, Long> {
    Optional<SupplierTarget> findBySupplierPublicIdAndProductPublicId(UUID publicId, UUID productPublicId);
}
