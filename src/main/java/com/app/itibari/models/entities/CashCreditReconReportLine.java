package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.CashCreditReconLineStatus;
import com.app.itibari.dataTypes.CashCreditReconAdjustmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "cash_credit_recon_report_lines", schema = "itibari")
public class CashCreditReconReportLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private CashCreditReconReport report;

    @Column(name = "route_public_id")
    private String routePublicId;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "delivery_agent_public_id")
    private String deliveryAgentPublicId;

    @Column(name = "delivery_agent_name")
    private String deliveryAgentName;

    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Column(name = "gmv_delivered")
    private BigDecimal gmvDelivered = BigDecimal.ZERO;

    @Column(name = "discounts")
    private BigDecimal discounts = BigDecimal.ZERO;

    @Column(name = "credit_discounts")
    private BigDecimal creditDiscounts = BigDecimal.ZERO;

    @Column(name = "complementaries")
    private BigDecimal complementaries = BigDecimal.ZERO;

    @Column(name = "net_gmv")
    private BigDecimal netGmv = BigDecimal.ZERO;

    @Column(name = "loading_offloading")
    private BigDecimal loadingOffloading = BigDecimal.ZERO;

    @Column(name = "credit_orders")
    private BigDecimal creditOrders = BigDecimal.ZERO;

    @Column(name = "parking_fee")
    private BigDecimal parkingFee = BigDecimal.ZERO;

    @Column(name = "ferry_cost")
    private BigDecimal ferryCost = BigDecimal.ZERO;

    @Column(name = "deposited_amount")
    private BigDecimal depositedAmount = BigDecimal.ZERO;

    @Column(name = "difference")
    private BigDecimal difference = BigDecimal.ZERO;

    @Column(name = "meta_outstanding")
    private BigDecimal metaOutstanding = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_status")
    private CashCreditReconLineStatus lineStatus = CashCreditReconLineStatus.BALANCED;

    @Column(name = "source_reconciliation_public_id")
    private String sourceReconciliationPublicId;

    @Column(name = "source_product_recon_public_id")
    private String sourceProductReconPublicId;

    @Where(clause = "deleted = false")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "reportLine", cascade = CascadeType.ALL)
    private List<CashCreditReconAdjustment> adjustments = new ArrayList<>();


    @Transient
    private BigDecimal totalExpenses = new BigDecimal("0.00");
    public BigDecimal getTotalExpenses() {
        if (adjustments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return adjustments.stream()
                .filter(adjustment -> adjustment.getApprovalStatus() == CashCreditReconAdjustmentStatus.APPROVED)
                .map(CashCreditReconAdjustment::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
