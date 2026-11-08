package com.app.itibari.repository;

import com.app.itibari.dataTypes.OperationTimingEntityType;
import com.app.itibari.dataTypes.OperationTimingStatus;
import com.app.itibari.models.entities.OperationTimingLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface OperationTimingLogRepository extends JpaRepository<OperationTimingLog, Long> {

    @Query("""
            SELECT log FROM OperationTimingLog log
            WHERE log.deleted = false
              AND (:entityType IS NULL OR log.entityType = :entityType)
              AND (:status IS NULL OR log.status = :status)
              AND (:routeId IS NULL OR LOWER(log.routeId) LIKE LOWER(CONCAT('%', :routeId, '%')))
              AND (:routePublicId IS NULL OR log.routePublicId = :routePublicId)
              AND (:startOfDate IS NULL OR log.creationTime >= :startOfDate)
              AND (:endOfDate IS NULL OR log.creationTime < :endOfDate)
            """)
    Page<OperationTimingLog> searchLogs(
            @Param("entityType") OperationTimingEntityType entityType,
            @Param("status") OperationTimingStatus status,
            @Param("routeId") String routeId,
            @Param("routePublicId") String routePublicId,
            @Param("startOfDate") LocalDateTime startOfDate,
            @Param("endOfDate") LocalDateTime endOfDate,
            Pageable pageable);
}
