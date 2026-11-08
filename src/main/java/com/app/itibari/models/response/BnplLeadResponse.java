package com.app.itibari.models.response;

import com.app.itibari.dataTypes.BnplFunnelStage;
import com.app.itibari.dataTypes.BnplLeadStatus;
import com.app.itibari.dataTypes.CustomerBnplVerificationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BnplLeadResponse {

    private UUID publicId;
    private UUID customerPublicId;
    private String customerName;
    private String phoneNumber;
    private BigDecimal loanEligibilityAmount;
    private BnplFunnelStage funnelStage;
    private BnplLeadStatus leadStatus;
    private String assignedAgentName;
    private Integer totalCallAttempts;
    private String scheduledDate;
    private UUID assignedAgentPublicId;
    private String latitude;
    private String longitude;
    private BigDecimal aov;
    private BigDecimal gmv;
    private String district;
    private String ward;
    private String outcome;
    private CustomerBnplVerificationStatus bnplVerificationStatus;
    private BigDecimal creditLimit = BigDecimal.ZERO;
    private CallStatusSummary callStatus;
    private CallStatusSummary outComeStatus;
    private LocalDateTime previousOrderDate;
    private SalesAgentDTO salesAgent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime lastCalledAt;

    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate lastFieldVisitDate;

    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate callbackDate;

    private Boolean callbackFlagged;
    private String lastReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime creationTime;
}
