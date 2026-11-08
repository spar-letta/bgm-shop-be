package com.app.itibari.models.entities;


import com.app.itibari.dataTypes.BatchStatus;
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
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "credit_item_batches", schema = "itibari")
public class CreditLimitBatches extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_customer_id", referencedColumnName = "id")
    private Customers parentCustomer;

    @ManyToMany
    @JoinTable(name = "batch_items", schema = "itibari",
            joinColumns = @JoinColumn(name = "batch_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "credit_item_id", referencedColumnName = "id"))
    private Set<CreditLimitItem> creditLimitItems = new HashSet<>();

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private BatchStatus batchStatus;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "total_amount")
    private BigDecimal totalAmount = new BigDecimal("0.00");

    @Column(name = "paid_amount")
    private BigDecimal paidAmount = new BigDecimal("0.00");

    @Column(name = "balance_amount")
    private BigDecimal balanceAmount = new BigDecimal("0.00");

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @Column(name = "invoice_generated_date")
    private LocalDate invoiceGeneratedDate;

    @Column(name = "payment_reference")
    private String paymentReference;
}
