package com.app.itibari.models.entities;


import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "credit_rules", schema = "itibari")
@NoArgsConstructor
@AllArgsConstructor
public class CreditRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    @Column(name = "id")
    private Long id;


    @JsonView(BaseView.DetailedView.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type")
    private CustomerType customerType;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "name")
    private String ruleName;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "is_credit_limit")
    private boolean creditLimit = false;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "credit_limit_percentage")
    private BigDecimal creditLimitPercentage = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "is_repayment_term")
    private boolean repaymentTerm = false;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "repayment_term_days")
    private Integer repaymentTermDays = 0;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "is_interest_rate")
    private boolean interestRate = false;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "interest_rate_percentage")
    private BigDecimal interestRatePercentage = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "is_overdue_fine")
    private boolean overdueFine;

    @JsonView(BaseView.DetailedView.class)
    @Column(name = "overdue_fine_percentage")
    private BigDecimal overdueFinePercentage = BigDecimal.ZERO;

    @JsonView(BaseView.DetailedView.class)
    @Transient
    private String condition;

    public String getCondition() {
        if (creditLimit) {
            return creditLimitPercentage + "%  of A.O.V";
        } else if (repaymentTerm) {
            return repaymentTermDays + " days";
        } else if (interestRate) {
            return interestRatePercentage + "%" + " interest rate to be incurred";
        } else if(overdueFine) {
            return overdueFinePercentage + "%" + " overdue fine to be incurred on overdue payments";
        } else {
            return "No condition set";
        }
    }
}
