package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.BnplFunnelStage;
import com.app.itibari.dataTypes.BnplLeadStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "bnpl_lead", schema = "itibari")
public class BnplLead extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    private Customers customer;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "loan_eligibility_amount", precision = 19, scale = 2)
    private BigDecimal loanEligibilityAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "funnel_stage")
    private BnplFunnelStage funnelStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_status")
    private BnplLeadStatus leadStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id", referencedColumnName = "id")
    private Person assignedAgent;

    @Column(name = "total_call_attempts")
    @Builder.Default
    private Integer totalCallAttempts = 0;

    @Column(name = "last_called_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime lastCalledAt;

    @Column(name = "last_field_visit_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate lastFieldVisitDate;

    @Column(name = "callback_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate callbackDate;

    @Column(name = "callback_flagged")
    @Builder.Default
    private Boolean callbackFlagged = Boolean.FALSE;

    @Column(name = "last_reason")
    private String lastReason;

    @Column(name = "customer_public_id")
    private String customerPublicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", referencedColumnName = "id")
    private SalesAgents salesAgent;

    @Column(name = "scheduled_date")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate scheduledDate;

    @OneToMany(mappedBy = "lead")
    private List<BnplFieldVisit> bnplFieldVisits = new ArrayList<>();


    @Column(name = "potential_credit_limit", precision = 19, scale = 2)
    private BigDecimal potentialCreditLimit = new BigDecimal("0.00");

    @Column(name = "call_status_public_id")
    private String callStatusPublicId;
}
