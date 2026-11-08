package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.CreditStatus;
import com.app.itibari.dataTypes.TransactionType;
import com.app.itibari.dataTypes.WalletItemStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "wallet", schema = "itibari")
@NoArgsConstructor
@AllArgsConstructor
public class Wallet extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    @Column(name = "id")
    private Long id;


    @JsonView(BaseView.DetailedView.class)
    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    private Customers customer;


    @JsonView(BaseView.DetailedView.class)
    @Column(name = "credit_status")
    @Enumerated(EnumType.STRING)
    private CreditStatus creditStatus;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "average_order_value")
    private BigDecimal averageOrderValue = BigDecimal.ZERO;


    @JsonView(BaseView.DetailedView.class)
    @Column(name = "credit_limit_balance")
    private BigDecimal creditLimitBalance = BigDecimal.ZERO;   // standing limit


    @JsonView(BaseView.DetailedView.class)
    @Column(name = "excess_amount_paid")
    private BigDecimal excessAmountPaid = BigDecimal.ZERO;   // excess amount paid by the customer

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "customer_id_number")
    private String customerID;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "tin_number")
    private String tinNumber;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "business_license_number")
    private String businessLicenseNumber;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "guarantor_name")
    private String guarantorName;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "guarantor_phone_number")
    private String guarantorPhoneNumber;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "wallet_balance")
    private BigDecimal walletBalance = BigDecimal.ZERO;



    @Where(clause = "deleted = false")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "wallet", cascade = CascadeType.ALL)
    private List<CreditLimitItem> creditLimitItems = new ArrayList<>();

    @Where(clause = "deleted = false")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "wallet", cascade = CascadeType.ALL)
    private List<PaymentItem> paymentItems = new ArrayList<>();

    @Where(clause = "deleted = false")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "wallet", cascade = CascadeType.ALL)
    private List<TransactionItem> transactionItems = new ArrayList<>();


    @JsonView(BaseView.DetailedView.class)
    @ManyToOne
    @JoinColumn(name = "blacklist_reason_id", referencedColumnName = "id")
    private RejectionsCode blacklistReason;

    @ManyToOne
    @JoinColumn(name = "parent_wallet_id", referencedColumnName = "id")
    private Wallet parentWallet;


    @JsonView(BaseView.DetailedView.class)
    @Transient
    private BigDecimal totalCreditAmount;

    @JsonView(BaseView.DetailedView.class)
    @Transient
    private BigDecimal totalPaymentAmount;

    @JsonView(BaseView.DetailedView.class)
    @Transient
    private BigDecimal totalTransactionAmount;

    @JsonView(BaseView.DetailedView.class)
    @Transient
    private BigDecimal limitUsage;


    @Transient
    @JsonView(BaseView.DetailedView.class)
    private BigDecimal unPaidAmount;

    @Transient
    @JsonView(BaseView.DetailedView.class)
    private BigDecimal totalCreditTaken;

    public BigDecimal getTotalCreditAmount() {
        return creditLimitItems.stream()
                .filter(creditLimitItem -> creditLimitItem.getStatus() != WalletItemStatus.CANCELLED)
                .map(CreditLimitItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPaymentAmount() {
        return paymentItems.stream()
                .filter(paymentItem -> paymentItem.getStatus() != WalletItemStatus.CANCELLED)
                .map(PaymentItem::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalTransactionAmount() {
        return transactionItems.stream()
                .filter(transactionItem -> transactionItem.getStatus() != WalletItemStatus.CANCELLED)
                .filter(item -> item.getTransactionType() == TransactionType.REPLACEMENT)
                .map(TransactionItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    // add helper methods to add items to the lists
    public void addCreditLimitItem(CreditLimitItem creditLimitItem) {
        this.creditLimitItems.add(creditLimitItem);
        creditLimitItem.setWallet(this);
    }

    public void addPaymentItem(PaymentItem paymentItem) {
        this.paymentItems.add(paymentItem);
        paymentItem.setWallet(this);
    }

    public void addTransactionItem(TransactionItem transactionItem) {
        this.transactionItems.add(transactionItem);
        transactionItem.setWallet(this);
    }


    public BigDecimal getLimitUsage() {
        if (creditLimitBalance == null || creditLimitBalance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (customer.getCreditLimit() != null && customer.getCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
            return customer.getCreditLimit().subtract(creditLimitBalance)
                    .divide(customer.getCreditLimit(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getUnPaidAmount() {
        BigDecimal upaidAmount = BigDecimal.ZERO;
        if (!creditLimitItems.isEmpty()) {
            for (CreditLimitItem creditLimitItem : creditLimitItems) {
                if (creditLimitItem.getStatus() != WalletItemStatus.PAID && creditLimitItem.getStatus() != WalletItemStatus.CANCELLED) {
                    upaidAmount = upaidAmount.add(creditLimitItem.getBalanceAmount());
                }
            }
        }
        return upaidAmount;
    }

    public BigDecimal getTotalCreditTaken() {
        BigDecimal totalCreditTaken = BigDecimal.ZERO;
        if (!creditLimitItems.isEmpty()) {
            for (CreditLimitItem creditLimitItem : creditLimitItems) {
                if (creditLimitItem.getStatus() != WalletItemStatus.CANCELLED) {
                    totalCreditTaken = totalCreditTaken.add(creditLimitItem.getAmount());
                }
            }
        }
        return totalCreditTaken;
    }


}
