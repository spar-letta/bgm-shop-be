package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class RoutePlanningOrderSlimDTO {

    private UUID publicId;
    private String salesOrderNumber;
    private String salesOrderStatus;
    private String deliveryStatus;
    private BigDecimal totalAmountPayable;
    private BigDecimal totalAmountDelivered;
    private BigDecimal totalDiscount;
    private boolean creditApplied;
    private BigDecimal customerAmountToPay;
    private String orderAmendStatus;
    private Boolean traReceiptGenerated;
    private CustomerSlimDTO customer;
}