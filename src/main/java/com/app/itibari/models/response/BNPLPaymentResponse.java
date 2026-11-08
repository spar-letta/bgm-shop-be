package com.app.itibari.models.response;

import com.app.itibari.dataTypes.CreditStatus;
import com.app.itibari.dataTypes.Batched;
import com.app.itibari.dataTypes.CustomerActiveStatus;
import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.dataTypes.PaymentStatusEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class BNPLPaymentResponse {
    private UUID publicId;
    private UUID creditLimitPublicId;
    private String customerName;
    private String customerPhoneNumber;
    private CustomerActiveStatus activeStatus;
    private CustomerType customerType;
    private BigDecimal creditAmount;
    private BigDecimal interestRate;
    private BigDecimal interestAmount;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;
    private PaymentStatusEnum paymentStatus;
    private LocalDate date;
    private String paymentReference;
    private CreditStatus creditStatus;
    private String duration;
    private long overdueDays;
    private BigDecimal creditLimit;
    private BigDecimal creditBalance;
    private LocalDate dateTaken;
    private LocalDate dueDate;
    private String orderNumber;
    private boolean orderDelivered;
    private BigDecimal amountPaidFromWallet;
    private BigDecimal walletBalance;
    private boolean enterpriseCredit;
    private Batched batched;
    private String salesAgent;
    private String walletPublicId;
}
