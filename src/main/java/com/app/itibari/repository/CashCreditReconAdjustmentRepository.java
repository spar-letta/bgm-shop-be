package com.app.itibari.repository;

import com.app.itibari.dataTypes.CashCreditReconAdjustmentType;
import com.app.itibari.dataTypes.CashCreditReconAdjustmentStatus;
import com.app.itibari.models.entities.CashCreditReconAdjustment;
import com.app.itibari.models.entities.CashCreditReconReportLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashCreditReconAdjustmentRepository extends JpaRepository<CashCreditReconAdjustment, Long> {

    List<CashCreditReconAdjustment> findByReportLineAndDeletedIsFalse(CashCreditReconReportLine reportLine);

    List<CashCreditReconAdjustment> findByReportLineAndApprovalStatusAndDeletedIsFalse(
            CashCreditReconReportLine reportLine,
            CashCreditReconAdjustmentStatus approvalStatus);

    Optional<CashCreditReconAdjustment> findByReportLineAndAdjustmentTypeAndDeletedIsFalse(
            CashCreditReconReportLine reportLine,
            CashCreditReconAdjustmentType adjustmentType);

    Optional<CashCreditReconAdjustment> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<CashCreditReconAdjustment> findByDeletedIsFalse(Pageable pageable);
}
