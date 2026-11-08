package com.app.itibari.models.response;

import com.app.itibari.dataTypes.CreditStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BNPLCustomerResponseDTO {
    private UUID publicId;
    private String customerName;
    private  String customerType;
    private LocalDate registrationDate;
    private CreditStatus creditStatus;
    private BigDecimal aov;
    private BigDecimal creditLimit;
    private String phoneNumber;
    private String region;
    private String zone;
    private BigDecimal limitUtilizationPercentage;
    private BigDecimal walletBalance;
    private String lastOrderDate;
    private Boolean isParent;
    private String parentCustomerName;
    private UUID parentCustomerPublicId;
    private List<BNPLCustomerResponseDTO> childCustomers;

}
