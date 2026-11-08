package com.app.itibari.models.request;

import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrdersRequest {

    private UUID customerId;

    private String sourceChannel;

    private String bookedBy;

    private boolean bookedBySalesAgent;

    private UUID salesAgentPublicId;

    private Set<SalesOrderItemsRequest> salesOrderItems;

    private String otp;

    private Boolean buyOnCredit;

    private boolean orderBookedByParent;


    public Boolean getBuyOnCredit() {
        return buyOnCredit!= null ? buyOnCredit : false;
    }

}
