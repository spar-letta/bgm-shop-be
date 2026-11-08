package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.PaymentMethod;
import com.app.itibari.dataTypes.RemittanceStatus;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "remittances", schema = "itibari")
public class Remittances extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.RemittanceSummaryView.class)
    @Column(name = "remittance_number")
    private String remittanceNumber;

    @JsonView(BaseView.RemittanceSummaryView.class)
    @Column(name = "paid_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate paidDate;

    @JsonView(BaseView.RemittanceSummaryView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_fk", referencedColumnName = "id")
    private Suppliers supplier;

    @JsonView(BaseView.RemittanceSummaryView.class)
    @Column(name = "purchase_order_number")
    private String purchaseOrderNumber;

    @JsonView(BaseView.RemittanceSummaryView.class)
    @Column(name = "invoice_number")
    private String invoiceNumber;

    @JsonView(BaseView.RemittanceSummaryView.class)
    @Column(name = "amount_due")
    private BigDecimal amountDue = BigDecimal.ZERO;

    @JsonView(BaseView.RemittanceSummaryView.class)
    @Column(name = "amount_paid")
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @JsonView(BaseView.RemittanceSummaryView.class)
    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @JsonView(BaseView.RemittanceSummaryView.class)
    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private RemittanceStatus status;

    @Column(name = "is_rebate")
    private boolean rebate = false;

    @JsonView(BaseView.RemittanceSummaryView.class)
    @Transient
    private String balance;

    public String getBalance() {

        if (amountDue != null && amountDue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balanceDue = amountDue.subtract(amountPaid).max(BigDecimal.ZERO);
            return "(" + balanceDue + ")";
        }
        return "(0.00)";
    }
}
