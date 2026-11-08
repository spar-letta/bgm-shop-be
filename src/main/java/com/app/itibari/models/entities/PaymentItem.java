package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.WalletItemStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "payment_item", schema = "itibari")
public class PaymentItem extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "amount")
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "payment_method")
    private String paymentMethod;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "payment_reference")
    private String paymentReference;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "description")
    private String description;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "reference_id")
    private String referenceId;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "order_number")
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", referencedColumnName = "id")
    private Wallet wallet;

    @JsonView(BaseView.DetailedView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WalletItemStatus status;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "receipt_number")
    private String receiptNumber;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "interest_rate")
    private  BigDecimal interestRate = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "total_interest")
    private  BigDecimal totalInterest = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "amount_paid_from_wallet")
    private BigDecimal amountPaidFromWallet = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "approved_by")
    private String approvedBy;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "excess_amount_paid")
    private BigDecimal excessAmountPaid = BigDecimal.ZERO;


    @Transient
    @JsonView(BaseView.DetailedView.class)
    private String createdByUser;


    public String getCreatedByUser() {
        return this.getCreatedBy() != null ? this.getCreatedBy().getFullName() : null;
    }
}
