package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.VarianceType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "reconciliation", schema = "itibari")
public class Reconciliation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "delivery_id")
    private String deliveryId;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "route_id")
    private String routePublicId;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "delivery_agent")
    private String deliveryAgent;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "delivery_agent_public_id")
    private String deliveryAgentPublicId;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "delivery_status")
    private String deliveryStatus;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "warehouse_house_status")
    private String warehouseHouseStatus;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "delivery_amount")
    private BigDecimal deliveryAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "reconciliation_amount")
    private BigDecimal reconciliationAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "reconciled")
    private boolean reconciled = false;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "product_recon_public_id")
    private String productReconPublicId;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "missing_amount")
    private BigDecimal missingAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "total_amount_picked")
    private BigDecimal totalAmountPicked = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "deposit_amount")
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "debit_note_paid_amount")
    private BigDecimal debitNotePaidAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Transient
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Transient
    private VarianceType variance;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "total_discount")
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "expense_amount")
    private BigDecimal expenseAmount = BigDecimal.ZERO;


    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "credit_amount")
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "walk_in")
    private boolean walkIn;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "locked")
    private boolean locked = false;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "reconciliation")
    private List<CreditRecon> creditRecons = new ArrayList<>();

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Transient
    private BigDecimal totalPayableAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Transient
    private boolean receiptRequired;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Transient
    private BigDecimal creditDiscount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    public String getCreatedByName(){
        return this.getCreatedBy() != null ? this.getCreatedBy().getFullName() : "--";
    }

    // add deposited amount

    public void addDepositAmount(BigDecimal amount) {

        if (amount != null) {
            this.depositAmount = this.depositAmount.add(amount);
        }
    }

    public BigDecimal getCreditDiscount(){
        if(creditRecons.isEmpty()){
            return BigDecimal.ZERO;
        }
        return creditRecons.stream().map(CreditRecon::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getOutstandingAmount() {

        if (walkIn == true){
            return BigDecimal.ZERO;
        }

        if (this.getTotalDiscount() != null) {
            return this.deliveryAmount.subtract(this.depositAmount).subtract(this.totalDiscount).subtract(this.creditAmount).subtract(this.expenseAmount);
        }
        return this.deliveryAmount.subtract(this.depositAmount).subtract(this.creditAmount).subtract(this.expenseAmount);
    }

    public VarianceType getVariance() {

        if (this.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
            return VarianceType.Excess;
        } else if (this.getOutstandingAmount().compareTo(BigDecimal.ZERO) < 0) {
            return VarianceType.Less;
        }
        return null;
    }

    public BigDecimal getTotalPayableAmount() {

        if (this.getTotalDiscount() != null) {
            return this.deliveryAmount.subtract(this.totalDiscount).subtract(this.creditAmount).subtract(getCreditDiscount());
        }
        return this.deliveryAmount.subtract(this.creditAmount);
    }


    public boolean isReceiptRequired() {
        return this.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    public void addCreditRecon(CreditRecon savedCreditRecon) {
        Optional<CreditRecon> existingCreditReconOpt = creditRecons.stream()
                .filter(cr -> cr.getPublicId().equals(savedCreditRecon.getPublicId()))
                .findFirst();

        if(existingCreditReconOpt.isEmpty()) {
            creditRecons.add(savedCreditRecon);
        }
    }
}
