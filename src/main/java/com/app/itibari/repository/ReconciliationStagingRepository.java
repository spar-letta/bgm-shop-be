package com.app.itibari.repository;

import com.app.itibari.models.entities.ReconciliationStaging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ReconciliationStagingRepository extends JpaRepository<ReconciliationStaging, Long> {
    Page<ReconciliationStaging> findByDeletedIsFalse(Pageable pageable);

    Optional<ReconciliationStaging> findByPublicIdAndDeletedFalse(UUID publicId);

    @Query("select rs from ReconciliationStaging rs " +
            "where rs.deleted = false " +
            "and rs.creationTime between :startDate and :endDate")
    Page<ReconciliationStaging> findByCreationTimeBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    Page<ReconciliationStaging> findByDeliveryAgentNameContainingIgnoreCaseAndDeletedFalse(String searchParam, Pageable pageable);
}
