package com.app.itibari.models.response;

import com.app.itibari.dataTypes.CustomerType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class BNPLEligibleCustomerDTO {
    private UUID publicId;
    private String customerName;
    private String customerPhoneNumber;
    private CustomerType customerType;
    private BigDecimal aov;
    private BigDecimal creditLimit;
    private boolean cashPurchaser;
    private boolean selfOnboarded;
    private String customerID;
    private String tinNumber;
    private String businessLicenseNumber;
    private String guarantorName;
    private String guarantorPhoneNumber;

    private String customerCreationDate;
    private Long daysSinceLastOrder;
    private Long totalOrders;
    private BigDecimal gmv;
    private boolean bnplOnboarded;
    private String lastActivityDate;
    private LocalDateTime previousOrderDate;
}
