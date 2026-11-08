package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.CashCreditReconReportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "cash_credit_recon_reports", schema = "itibari")
public class CashCreditReconReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_status")
    private CashCreditReconReportStatus reportStatus = CashCreditReconReportStatus.GENERATED;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "reconciled_by")
    private String reconciledBy;

    @Column(name = "reconciliation_comment")
    private String reconciliationComment;

    @Column(name = "total_gmv_delivered")
    private BigDecimal totalGmvDelivered = BigDecimal.ZERO;

    @Column(name = "total_discounts")
    private BigDecimal totalDiscounts = BigDecimal.ZERO;

    @Column(name = "total_credit_discounts")
    private BigDecimal totalCreditDiscounts = BigDecimal.ZERO;

    @Column(name = "total_complementaries")
    private BigDecimal totalComplementaries = BigDecimal.ZERO;

    @Column(name = "total_net_gmv")
    private BigDecimal totalNetGmv = BigDecimal.ZERO;

    @Column(name = "total_loading_offloading")
    private BigDecimal totalLoadingOffloading = BigDecimal.ZERO;

    @Column(name = "total_credit_orders")
    private BigDecimal totalCreditOrders = BigDecimal.ZERO;

    @Column(name = "total_parking_fee")
    private BigDecimal totalParkingFee = BigDecimal.ZERO;

    @Column(name = "total_ferry_cost")
    private BigDecimal totalFerryCost = BigDecimal.ZERO;

    @Column(name = "total_deposited_amount")
    private BigDecimal totalDepositedAmount = BigDecimal.ZERO;

    @Column(name = "total_difference")
    private BigDecimal totalDifference = BigDecimal.ZERO;

    @Column(name = "total_meta_outstanding")
    private BigDecimal totalMetaOutstanding = BigDecimal.ZERO;

    @Where(clause = "deleted = false")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "report", cascade = CascadeType.ALL)
    private List<CashCreditReconReportLine> lines = new ArrayList<>();
}
