package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.Batched;
import com.app.itibari.dataTypes.CreditStatus;
import com.app.itibari.dataTypes.PaymentStatusEnum;
import com.app.itibari.dataTypes.WalletItemStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "credit_items", schema = "itibari")
public class CreditLimitItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    //@JsonView(BaseView.DetailedView.class)
    @Column(name = "total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    //@JsonView(BaseView.DetailedView.class)
    @Column(name = "reference_id")
    private String referenceId;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "description")
    private String description;

    //@JsonView(BaseView.DetailedView.class)
    @Column(name = "confirmed_amount")
    private BigDecimal confirmedAmount = BigDecimal.ZERO;

    // @JsonView(BaseView.DetailedView.class)
    @Column(name = "total_confirmed_amount")
    private BigDecimal totalConfirmedAmount = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "paid_amount")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "order_number")
    private String orderNumber;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "payment_reference")
    private String paymentReference;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "interest_rate")
    private BigDecimal interestRate = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "interest_amount")
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "excess_amount_paid")
    private BigDecimal excessAmountPaid = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "fine_amount")
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "amount_paid_from_wallet")
    private BigDecimal amountPaidFromWallet = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "fully_paid")
    private boolean fullyPaid = false;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "credit_status")
    @Enumerated(EnumType.STRING)
    private CreditStatus creditStatus;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatusEnum paymentStatus;


    @JsonView(BaseView.DetailedView.class)
    @Column(name = "date_taken")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate dateTaken;


    @JsonView(BaseView.DetailedView.class)
    @Column(name = "due_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate dueDate;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private WalletItemStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", referencedColumnName = "id")
    private Wallet wallet;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "order_delivered")
    private boolean orderDelivered = false;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "cancelled")
    private boolean cancelled = false;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "order_public_id")
    private String orderPublicId;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "reminder_sent")
    private boolean reminderSent = false;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "has_excess_payment")
    private boolean hasExcessPayment = false;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "pay_via_wallet")
    private boolean payViaWallet = false;

    @Column(name = "has_fine")
    private boolean hasFine = false;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "fine_percentage")
    private BigDecimal finePercentage = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "reconciled")
    private boolean reconciled = false;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "reconciliation_comment")
    private String reconciliationComment;

    @Column(name = "parent_customer_public_id")
    private String parentCustomerPublicId;

    @Column(name = "batch_public_id")
    private String batchPublicId;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "batched")
    @Enumerated(EnumType.STRING)
    private Batched batched = Batched.NOT_BATCHED;


    @JsonView(BaseView.DetailedView.class)
    @Column(name = "discount")
    private BigDecimal discount = new BigDecimal("0.00");


    @JsonView(BaseView.DetailedView.class)
    @Transient
    private String durationDays;


    @JsonView(BaseView.DetailedView.class)
    @Transient
    private BigDecimal balanceAmount;


    @JsonView(BaseView.DetailedView.class)
    @Transient
    private long overdueDays;

    @Transient
    private BigDecimal totalAmountWithOutInterest;


    @Transient
    @JsonView(BaseView.DetailedView.class)
    private BigDecimal walletBalance = BigDecimal.ZERO;


    @Transient
    @JsonView(BaseView.DetailedView.class)
    private boolean parent;

    @Transient
    @JsonView(BaseView.DetailedView.class)
    private boolean childCreditLimitItem;

    @Transient
    @JsonView(BaseView.DetailedView.class)
    private String createdByUser;


    public String getCreatedByUser() {
        return this.getCreatedBy() != null ? this.getCreatedBy().getFullName() : null;
    }


    public BigDecimal getBalanceAmount() {
        BigDecimal fine = this.fineAmount == null ? BigDecimal.ZERO : this.fineAmount;
        if (!hasExcessPayment) {
            return this.amount.add(this.interestAmount).add(fine).subtract(this.paidAmount);
        }
        return BigDecimal.ZERO;
    }

    public String getDurationDays() {
        if (this.getCreationTime() != null) {
            long daysBetween = ChronoUnit.DAYS.between(this.getCreationTime().toLocalDate(), LocalDate.now());
            return daysBetween + " days";
        }
        return "Already Paid";
    }

    public long getOverdueDays() {
        if (dueDate != null && LocalDate.now().isAfter(dueDate)) {
            return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        }
        return 0;
    }


    public BigDecimal getTotalAmountWithOutInterest() {
        if (this.paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            return this.paidAmount.subtract(this.interestAmount);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getWalletBalance() {
        if (wallet != null) {
            return wallet.getWalletBalance();
        }
        return BigDecimal.ZERO;
    }

    public boolean isParent() {
        return wallet.getParentWallet() != null;
    }

    public boolean isChildCreditLimitItem() {
        return wallet != null
                && wallet.getCustomer() != null
                && wallet.getCustomer().getParentCustomer() != null;
    }

}
