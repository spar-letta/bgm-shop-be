package com.app.itibari.repository;

import com.app.itibari.models.entities.Quarantine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface QuarantineRepository extends JpaRepository<Quarantine, Long>, JpaSpecificationExecutor<Quarantine> {

    Optional<Quarantine> findByPublicIdAndDeletedIsFalse(UUID publicId);
}
