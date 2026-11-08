package com.app.itibari.models.request;

import com.app.itibari.models.response.CategorySummaryDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TRARequest {



    // You can also have a provision to save client without tin and the custidtype can be sent as 6 to the vfd this automatically means NIL and you dont need to send 999999999.
    // (This can be done by the development team)
    @JsonProperty("idate")
    private String iDate;

    @JsonProperty("itime")
    private String iTime;

    @JsonProperty("custinvoiceno")
    private String custInvoiceNo;

    @NotNull(message = "tin is required")
    @JsonProperty("custidtype")
    private String custIdType;  // this will 6 and don't need to send custId as 999999999.

    @NotNull(message = "tin is required")
    @JsonProperty("custid")
    private String custId;

    @JsonProperty("custname")
    private String custName;

    @JsonProperty("mobilenum")
    private String mobileNum;

    @JsonProperty("username")
    private String userName;

    @JsonProperty("branch")
    private String branch;

    @JsonProperty("department")
    private String department;

    @JsonProperty("devicenumber")
    private String deviceNumber;

    @NotNull
    @JsonProperty("paytype")
    private String payType;

    @JsonProperty("fcode")
    private String fCode;

    @JsonProperty("fcodetoken")
    private String fCodeToken;

    @JsonIgnore
//    @JsonProperty("invoiceDetails")
    private List<InvoiceDetailDto> oldInvoiceDetails = new ArrayList<>();

    @JsonProperty("invoiceDetails")
    private List<CategorySummaryDto> invoiceDetails = new ArrayList<>();

    @JsonIgnore
    private String originalCustomerName;
}
