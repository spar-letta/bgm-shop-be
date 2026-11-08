package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.TransactionType;
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
@Table(name = "transaction_item", schema = "itibari")
public class TransactionItem extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "transaction_type")
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "amount")
    private BigDecimal amount;

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
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private WalletItemStatus status;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "payment_pending")
    private boolean paymentPending;
}
