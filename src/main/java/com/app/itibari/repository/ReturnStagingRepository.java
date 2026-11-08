package com.app.itibari.repository;

import com.app.itibari.models.entities.ReturnStaging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReturnStagingRepository extends JpaRepository<ReturnStaging, Long> {

    Optional<ReturnStaging> findByPublicIdAndDeletedIsFalse(UUID returnStagePublicId);
}
