package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class FileReceiptDTO {

    private String url;

    private String contentType;

    private String description;

    private String orderNumber;

    private String receiptNumber;

    private String status;

    private String rejectionReason;

    private BigDecimal amount;

    private String date;
}
