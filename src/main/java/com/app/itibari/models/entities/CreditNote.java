package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.CreditNoteOrderAction;
import com.app.itibari.dataTypes.CreditNoteStatus;
import com.app.itibari.dataTypes.CreditStatus;
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

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "credit_note", schema = "itibari")
public class CreditNote extends  BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.CreditNoteSummaryView.class)
    @Column(name = "amount")
    private BigDecimal amount;

    @JsonView(BaseView.CreditNoteSummaryView.class)
    @Column(name = "note")
    private String note;

    @JsonView(BaseView.CreditNoteSummaryView.class)
    @Column(name = "approved_by")
    private String actionedBy;

    @JsonView(BaseView.CreditNoteSummaryView.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id_fk",referencedColumnName = "id")
    private Suppliers supplier;

    @JsonView(BaseView.CreditNoteSummaryView.class)
    @Column(name = "credit_note_number")
    private String creditNoteNumber;

    @JsonView(BaseView.CreditNoteSummaryView.class)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CreditNoteStatus status;

    @JsonView(BaseView.CreditNoteSummaryView.class)
    @Column(name = "purchase_order_public_id")
    private String purchaseOrderPublicId;

    @JsonView(BaseView.CreditNoteSummaryView.class)
    @Column(name = "action_applied")
    @Enumerated(EnumType.STRING)
    private CreditNoteOrderAction actionApplied;

    @JsonView(BaseView.CreditNoteSummaryView.class)
    @Column(name = "tagged")
    private Boolean tagged = false;

    @Column(name = "tagged_purchase_order_public_id")
    private String taggedPurchaseOrderPublicId;

    @JsonView(BaseView.CreditNoteSummaryView.class)
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @Column(name = "credit_note_date")
    private LocalDate creditNoteDate;

    @JsonView(BaseView.CreditNoteSummaryView.class)
    @Column(name = "supplier_raised")
    private boolean supplierRaised =  false;
}
