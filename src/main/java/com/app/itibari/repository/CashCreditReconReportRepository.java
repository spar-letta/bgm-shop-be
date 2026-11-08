package com.app.itibari.repository;

import com.app.itibari.dataTypes.CashCreditReconReportStatus;
import com.app.itibari.models.entities.CashCreditReconReport;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CashCreditReconReportRepository extends JpaRepository<CashCreditReconReport, Long> {

    Optional<CashCreditReconReport> findByPublicIdAndDeletedIsFalse(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CashCreditReconReport> findFirstByReportDateAndReportStatusNotAndDeletedIsFalseOrderByCreationTimeDesc(
            LocalDate reportDate,
            CashCreditReconReportStatus reportStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from CashCreditReconReport report where report.publicId = :publicId and report.deleted = false")
    Optional<CashCreditReconReport> findByPublicIdAndDeletedIsFalseForUpdate(@Param("publicId") UUID publicId);
}
