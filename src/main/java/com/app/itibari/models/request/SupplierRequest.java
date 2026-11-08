package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SupplierRequest {

    private String supplierName;

    private String supplierAddress;

    private String supplierPointOfContact;

    private String phoneNumber;

    private String email;

    private Integer leadDays;

    private Integer creditTerms;

    private String TinNumber;

    private BigDecimal creditLimit;

    private String postalAddress;

    private String BankName;

    private String BankAccountName;

    private String BankAccountNumber;
}
