package com.app.itibari.models.request;

import com.app.itibari.dataTypes.AmountType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ApprovalRequest {

    private ApprovalAction action;

    private String comment;

    private Boolean hasSpecialDiscount;

    private AmountType amountType;

    private BigDecimal amount;

    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate endDate;

    private List<UUID> productIds;

    // once a special discount is approved
    private List<ApprovedProductItems> productItems;

    public Boolean getHasSpecialDiscount() {

        return hasSpecialDiscount != null ? hasSpecialDiscount : false;
    }

    public enum ApprovalAction {
        APPROVE,
        REJECT
    }
}
