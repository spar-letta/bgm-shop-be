package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.CreditStatus;
import com.app.itibari.dataTypes.PaymentStatusEnum;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "credit_recon", schema = "itibari")
public class CreditRecon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_public_id")
    private String orderPublicId;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "order_number")
    private String orderNumber;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "customer_public_id")
    private String customerPublicId;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "customer_name")
    private String customerName;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "sales_agent_public_id")
    private String salesAgentPublicId;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "sales_agent_name")
    private String salesAgentName;

    @Column(name = "delivery_agent_public_id")
    private String deliveryAgentPublicId;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "delivery_agent_name")
    private String deliveryAgentName;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "credit_amount")
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "interest_amount")
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "interest_rate")
    private BigDecimal interestRate = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "paid_amount")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "order_total_amount")
    private BigDecimal orderTotalAmount = BigDecimal.ZERO;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "route_id")
    private String routeId;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "otp_code")
    private String otpCode;

    @Where(clause = "deleted = false")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "creditRecon", cascade = CascadeType.ALL)
    private List<CreditReconItem> creditReconItems = new ArrayList<>();


    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "deposit_amount")
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "balance_amount")
    @JsonView(BaseView.ReconciliationSummaryView.class)
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    @Column(name = "credit_status")
    @Enumerated(EnumType.STRING)
    @JsonView(BaseView.ReconciliationSummaryView.class)
    private CreditStatus creditStatus;


    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "reconciliation_comment")
    private String reconciliationComment;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "reconciled")
    private boolean reconciled = false;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "order_creation_date_time")
    private LocalDateTime orderCreationDateTime;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "paid_date_time")
    private LocalDateTime paidDateTime;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatusEnum paymentStatus;

    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Column(name = "payment_reference")
    private String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id",referencedColumnName = "id")
    private Reconciliation reconciliation;


    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Transient
    public BigDecimal getGrandTotal() {
        return interestAmount.add(creditAmount);
    }


    @JsonView(BaseView.ReconciliationSummaryView.class)
    @Transient
    public BigDecimal getDiscountAmount() {
        BigDecimal discount = BigDecimal.ZERO;
        if (!creditReconItems.isEmpty()) {
            for (CreditReconItem item : creditReconItems) {
                if (item.getDiscountAmount() != null) {
                    discount = discount.add(item.getDiscountAmount());
                }
            }
        }
        return discount;
    }

    public void addCreditReconItem(CreditReconItem item) {
        Optional<CreditReconItem> existingItemOpt = creditReconItems.stream()
                .filter(i -> i.getProductPublicId().equals(item.getProductPublicId()))
                .findFirst();

        if (existingItemOpt.isEmpty()) {
            item.setCreditRecon(this);
            creditReconItems.add(item);
        }
    }


}
