package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.BnplCreditLimitAdjustmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "bnpl_credit_limit_adjustments", schema = "itibari")
public class BnplCreditLimitAdjustment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    private Customers customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", referencedColumnName = "id")
    private Wallet wallet;

    @Column(name = "adjustment_type")
    @Enumerated(EnumType.STRING)
    private BnplCreditLimitAdjustmentType adjustmentType;

    @Column(name = "percentage")
    private BigDecimal percentage = BigDecimal.ZERO;

    @Column(name = "old_credit_limit")
    private BigDecimal oldCreditLimit = BigDecimal.ZERO;

    @Column(name = "new_credit_limit")
    private BigDecimal newCreditLimit = BigDecimal.ZERO;

    @Column(name = "old_credit_limit_balance")
    private BigDecimal oldCreditLimitBalance = BigDecimal.ZERO;

    @Column(name = "new_credit_limit_balance")
    private BigDecimal newCreditLimitBalance = BigDecimal.ZERO;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "triggered_by_credit_item_ids")
    private String triggeredByCreditItemIds;

    @Column(name = "reason")
    private String reason;
}
