package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.ChurnedCustomerLeadStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "churned_call_histories", schema = "itibari")
public class ChurnedCallHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "creation_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime creationTime;

    @Column(name = "date")
    private String date;

    @Column(name = "customer_public_id")
    private String customerPublicId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "shop_name")
    private String shopName;

    @Column(name = "district")
    private String district;

    @Column(name = "ward")
    private String ward;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "last_order_date")
    private String lastOrderDate;

    @Column(name = "cx_call_date")
    private String cxCallDate;

    @Column(name = "cx_agent_name")
    private String cxAgentName;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "xc_note")
    private String xcNote;

    @Column(name = "recommended_out_come")
    private String recommendedOutCome;

    @Column(name = "responsible_department")
    private String responsibleDepartment;

    @Column(name = "order_placed_after_recommented_outcome")
    private boolean orderPlacedAfterRecommentedOutcome;

    @Column(name = "order_date")
    private String orderDate;

    @Column(name = "cx_agent_public_id")
    private UUID cxAgentPublicId;

    @Column(name = "is_churned")
    private boolean isChurned;

    @Column(name = "location_change")
    private boolean locationChange;

    @Column(name = "order_amendment")
    private boolean orderAmendment;

    @Column(name = "is_buy_now_pay_later")
    private boolean isBuyNowPayLater;

    @Column(name = "call_status")
    private String callStatus;

    @Column(name = "call_status_date_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime callStatusDateTime;

    @Column(name = "amendment_public_id")
    private UUID amendmentPublicId;

    @Column(name = "call_response_status")
    private boolean callResponseStatus;

    @Column(name = "telephone")
    private boolean telephone;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_status")
    private ChurnedCustomerLeadStatus leadStatus;
}
