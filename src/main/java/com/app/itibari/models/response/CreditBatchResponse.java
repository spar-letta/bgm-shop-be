package com.app.itibari.models.response;

import com.app.itibari.dataTypes.BatchStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreditBatchResponse {
    private UUID publicId;
    private String customer;
    private String customerPublicId;
    private String batchNumber;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private BatchStatus  status;
    private LocalDate createdDate;
    private String createdBy;
    private LocalDate paymentDate;
    private String invoiceNumber;
    private LocalDate invoiceGeneratedDate;
    private String paymentReference;

}
