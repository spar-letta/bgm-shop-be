package com.app.itibari.repository;

import com.app.itibari.dataTypes.CashCreditReconReportStatus;
import com.app.itibari.models.entities.CashCreditReconReport;
import com.app.itibari.models.entities.CashCreditReconReportLine;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.Column;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashCreditReconReportLineRepository extends JpaRepository<CashCreditReconReportLine, Long> {

    List<CashCreditReconReportLine> findByReportAndDeletedIsFalse(CashCreditReconReport report);

    Optional<CashCreditReconReportLine> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<CashCreditReconReportLine> findByReportAndRoutePublicIdAndDeletedIsFalse(CashCreditReconReport report, String routePublicId);

    boolean existsBySourceReconciliationPublicIdAndReport_ReportStatusAndDeletedIsFalse(
            String sourceReconciliationPublicId,
            CashCreditReconReportStatus reportStatus);

    boolean existsBySourceProductReconPublicIdAndReport_ReportStatusAndDeletedIsFalse(
            String sourceProductReconPublicId,
            CashCreditReconReportStatus reportStatus);


    // CashCreditReconReportLine findByDeliveryAgentPublicIdAndDeletedIsFalse(String deliveryAgentId);

    Optional<CashCreditReconReportLine> findByDeliveryAgentPublicIdAndRoutePublicIdAndDeletedIsFalse(String deliveryAgentId, String string);

    // In your Repository
    @Query(value = """
        SELECT line
        FROM CashCreditReconReportLine line
        WHERE line.deleted = false
          AND EXISTS (
              SELECT 1
              FROM line.adjustments adjustment
              WHERE adjustment.deleted = false
          )
          AND (:deliveryAgentAgent IS NULL OR line.deliveryAgentPublicId = :deliveryAgentAgent)
          AND (CAST(:searchParam AS String) IS NULL OR LOWER(line.routeId) LIKE LOWER(CONCAT('%', CAST(:searchParam AS String), '%')))
          AND (CAST(:fromDate AS timestamp) IS NULL OR line.creationTime >= :fromDate)
          AND (CAST(:toDate AS timestamp) IS NULL OR line.creationTime <= :toDate)
        """,
            countQuery = """
        SELECT COUNT(line)
        FROM CashCreditReconReportLine line
        WHERE line.deleted = false
          AND EXISTS (
              SELECT 1
              FROM line.adjustments adjustment
              WHERE adjustment.deleted = false
          )
          AND (:deliveryAgentAgent IS NULL OR line.deliveryAgentPublicId = :deliveryAgentAgent)
          AND (CAST(:searchParam AS String) IS NULL OR LOWER(line.routeId) LIKE LOWER(CONCAT('%', CAST(:searchParam AS String), '%')))
          AND (CAST(:fromDate AS timestamp) IS NULL OR line.creationTime >= :fromDate)
          AND (CAST(:toDate AS timestamp) IS NULL OR line.creationTime <= :toDate)
        """)
    Page<CashCreditReconReportLine> findLinesWithAdjustments(
            @Param("deliveryAgentAgent") String deliveryAgentAgent,
            @Param("searchParam") String searchParam,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
