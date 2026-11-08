package com.app.itibari.repository;

import com.app.itibari.dataTypes.ReconStatus;
import com.app.itibari.models.entities.ProductRecon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductReconRepository extends JpaRepository<ProductRecon, Long>, JpaSpecificationExecutor<ProductRecon> {

    Optional<ProductRecon> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<ProductRecon> findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(String agentPublicId,
                                                                                                         String deliveryId);

    Optional<ProductRecon> findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(String routeId);

    List<ProductRecon> findAllByCreationTimeBetweenAndDeletedIsFalse(LocalDateTime startTime, LocalDateTime endTime);

    List<ProductRecon> findByDeletedFalseAndReconciledTrueAndStatusAndCreationTimeBetween(
            ReconStatus status, LocalDateTime startDate, LocalDateTime endDate);

    Optional<ProductRecon> findByCreationTimeBetweenAndWalkInIsTrueAndDeletedIsFalse(LocalDateTime startOfDay, LocalDateTime endOfDay);
}
