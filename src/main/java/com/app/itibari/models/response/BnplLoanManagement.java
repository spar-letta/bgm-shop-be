package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BnplLoanManagement {
    private String creditPublicId;
    private String customerPublicId;
    private String orderPublicId;
    private String orderNumber;
    private String customerName;
    private String customerActiveStatus;
    private String customerPhoneNumber;
    private String customerEmail;
    private BigDecimal creditTakenAmount;
    private BigDecimal interestRate;
    private BigDecimal interestAmount;
    private BigDecimal dueAmount;
    private String creditDate;
    private String paymentDueDate;
    private String paymentStatus;
    private boolean reminderSent;
    private boolean defaulted;
    private String paymentDate;
    private BigDecimal amountPaidFromWallet;
}
