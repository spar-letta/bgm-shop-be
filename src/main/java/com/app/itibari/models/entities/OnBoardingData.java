package com.app.itibari.models.entities;

import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
public class OnBoardingData {

    @JsonView(BaseView.CustomersView.class)
    @Column(name = "customer_id_number")
    private String customerID;


    @JsonView(BaseView.CustomersView.class)
    @Column(name = "tin_number")
    private String tinNumber;


    @JsonView(BaseView.CustomersView.class)
    @Column(name = "business_license_number")
    private String businessLicenseNumber;


    @JsonView(BaseView.CustomersView.class)
    @Column(name = "guarantor_name")
    private String guarantorName;


    @JsonView(BaseView.CustomersView.class)
    @Column(name = "guarantor_phone_number")
    private String guarantorPhoneNumber;

}
