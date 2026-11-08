package com.app.itibari.repository;

import com.app.itibari.models.entities.Reconciliation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, Long>, JpaSpecificationExecutor<Reconciliation> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Reconciliation d where d.publicId = :publicId and d.deleted = false")
    Optional<Reconciliation> findByPublicIdForUpdate(@Param("publicId") UUID publicId);

    Optional<Reconciliation> findByPublicIdAndDeletedIsFalse(UUID reconciliationId);

    Optional<Reconciliation> findByProductReconPublicIdAndDeletedIsFalse(String string);

    Optional<Reconciliation> findByRoutePublicIdAndDeletedIsFalse(String string);

    Reconciliation findByDeliveryIdIgnoreCase(String deliveryId);
}
