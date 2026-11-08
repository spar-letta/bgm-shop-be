package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.CashCreditReconAdjustmentType;
import com.app.itibari.dataTypes.CashCreditReconAdjustmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "cash_credit_recon_adjustments", schema = "itibari")
public class CashCreditReconAdjustment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_line_id")
    private CashCreditReconReportLine reportLine;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type")
    private CashCreditReconAdjustmentType adjustmentType;

    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "note")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private CashCreditReconAdjustmentStatus approvalStatus = CashCreditReconAdjustmentStatus.PENDING;

    @Column(name = "review_comment")
    private String reviewComment;

    @Column(name = "attachment_url")
    private String attachmentUrl;
}
