package com.app.itibari.repository;

import com.app.itibari.models.entities.ReminderJob;
import com.app.itibari.models.entities.ReminderLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    Page<ReminderLog> findAllByDeletedIsFalse(Pageable pageable);


    boolean existsByReminderJobAndOrderNumber(ReminderJob job, String orderNumber);

    Optional<ReminderLog> findByPublicIdAndDeletedIsFalse(UUID publicId);

    @Query("""
            SELECT l FROM ReminderLog l
            LEFT JOIN l.customer c
            WHERE l.deleted = false
              AND (:search IS NULL OR
                   LOWER(l.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:from IS NULL OR l.sentAt >= :from)
              AND (:to   IS NULL OR l.sentAt <= :to)
            """)
    Page<ReminderLog> searchLogs(
            @Param("search") String search,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
