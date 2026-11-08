package com.app.itibari.models.response;

import com.app.itibari.dataTypes.CreditStatus;
import com.app.itibari.dataTypes.CustomerType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class WalletResponseDTO {
    private UUID publicId;
    private String customerName;
    private CustomerType customerType;
    private String region;
    private String zone;
    private LocalDate onBoardingDate;
    private String idNumber;
    private CreditStatus creditStatus;
    private String customerID;
    private String tinNumber;
    private String businessLicenseNumber;
    private String guarantorName;
    private String guarantorPhoneNumber;
    private BigDecimal availableBalance;
    private BigDecimal creditLimit;
    private BigDecimal totalCreditUsed;
    private BigDecimal totalPaymentsMade;
    private BigDecimal unpaidAmount;
    private BigDecimal walletBalance;  // excess amount paid
    private BigDecimal totalCreditTaken;

}
