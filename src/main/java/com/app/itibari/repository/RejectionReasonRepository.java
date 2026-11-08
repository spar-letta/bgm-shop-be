package com.app.itibari.repository;

import com.app.itibari.models.entities.RejectionReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RejectionReasonRepository extends JpaRepository<RejectionReason, Long> {
    Optional<RejectionReason> findByPublicIdAndDeletedIsFalse(UUID publicId);
}
