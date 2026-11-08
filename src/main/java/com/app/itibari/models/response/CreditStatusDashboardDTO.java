package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreditStatusDashboardDTO {
    private Integer customersOnCredit;
    private BigDecimal averageCreditLimit;
    private Integer totalCreditOrders;
    private Integer healthyCreditCustomers;
    private Integer churnedCreditCustomers;
    private Integer defaulterCreditCustomers;
    private Integer blacklistedCreditCustomers;
}
