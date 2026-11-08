package com.app.itibari.models.request;

import com.app.itibari.dataTypes.CustomerType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateCreditRuleRequest {
    private CustomerType customerType;
    private String ruleName;
    private Boolean isCreditLimit;
    private BigDecimal creditLimitPercentage;
    private Boolean isRepaymentTerm;
    private Integer repaymentTermDays;
    private Boolean isInterestRate;
    private BigDecimal interestRatePercentage;
    private Boolean isOverdueFine;
    private BigDecimal overdueFinePercentage;


    public Boolean getIsCreditLimit() {
        return isCreditLimit != null ? isCreditLimit : false;
    }

    public Boolean getIsRepaymentTerm() {
        return isRepaymentTerm != null ? isRepaymentTerm : false;
    }

    public Boolean getIsInterestRate() {
        return isInterestRate != null ? isInterestRate : false;
    }

    public Boolean getIsOverdueFine() {
        return isOverdueFine != null ? isOverdueFine : false;
    }
}
