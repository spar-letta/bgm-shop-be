package com.app.itibari.repository;

import com.app.itibari.models.entities.ReconciliationStagingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReconciliationStagingItemRepository extends JpaRepository<ReconciliationStagingItem, Long> {
    Optional<ReconciliationStagingItem> findByPublicIdAndDeletedIsFalse(UUID publicId);
}
