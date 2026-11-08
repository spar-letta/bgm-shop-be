package com.app.itibari.repository;

import com.app.itibari.models.entities.HelpDeskNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HelpDeskNotificationRepository extends JpaRepository<HelpDeskNotification, Long> {
    Page<HelpDeskNotification> findAllByAssignedToPublicId(UUID assignedId, Pageable pageable);

    Optional<HelpDeskNotification> findByPublicId(UUID ticketNotificationPublicId);
}
