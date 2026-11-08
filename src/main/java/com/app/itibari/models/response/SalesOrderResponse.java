package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class SalesOrderResponse {
    private UUID publicId;
    private String orderNumber;
    private String customer;
    private String customerPublicId;
    private BigDecimal totalAmount;
    private String status;
    private String creationDate;
}
