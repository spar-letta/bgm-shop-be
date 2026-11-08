package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.DebitReferenceType;
import com.app.itibari.dataTypes.DebtZone;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "debit_notes", schema = "itibari")
public class DebitNotes extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "reference_id")
    private UUID referenceId;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "debit_reference_type")
    @Enumerated(EnumType.STRING)
    private DebitReferenceType referenceType;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "delivery_agent_name")
    private String deliveryAgentName;

    @JsonView(BaseView.DebitNotesView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "debt_level")
    private DebtZone debtLevel;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "delivery_agent_public_id")
    private UUID deliveryAgentPublicId;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "description")
    private String description;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "amount")
    private BigDecimal amount;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "balance")
    private BigDecimal balance;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "date_listed")
    private LocalDateTime whiteListingDate;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "white_listed_by")
    private String whiteListedBy;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "route")
    private String route;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "route_public_id")
    private String routePublicId;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "reconciliation_public_id")
    private String reconciliationPublicId;

    @JsonView(BaseView.DebitNotesView.class)
    @Column(name = "reconciled")
    private Boolean reconciled = false;


    public String getStatus() {
        return Boolean.TRUE.equals(reconciled) ? "RECONCILED" : "PENDING";
    }

    public UUID getDeliveryAgentPublicId() {
        return referenceType == DebitReferenceType.DeliveryAgent ? referenceId : deliveryAgentPublicId;
    }

    private void synchronizeDerivedFields() {
        deliveryAgentPublicId = getDeliveryAgentPublicId();
        status = getStatus();
    }

}
