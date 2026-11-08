package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.BnplCallStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "bnpl_call_attempt", schema = "itibari")
public class BnplCallAttempt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", referencedColumnName = "id", nullable = false)
    private BnplLead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", referencedColumnName = "id", nullable = false)
    private Person agent;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "start_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_status")
    private BnplCallStatus callStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "bnpl_call_attempt_reason_codes",
            schema = "itibari",
            joinColumns = @JoinColumn(name = "call_attempt_id"),
            inverseJoinColumns = @JoinColumn(name = "reason_code_id")
    )
    private List<BnplReasonCode> reasonCodes;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outcome_status_id", referencedColumnName = "id")
    private CallStatus outComeStatus;
}