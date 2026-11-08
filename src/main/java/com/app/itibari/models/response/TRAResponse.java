package com.app.itibari.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TRAResponse {

    @JsonProperty("rctvnum")
    private String rctvNum;

    @JsonProperty("rctvcode")
    private String rctvCode;

    @JsonProperty("znumber")
    private String zNumber;

    @JsonProperty("vfdinvoicenum")
    private String vfdInvoiceNum;

    @JsonProperty("idate")
    private String iDate;

    @JsonProperty("itime")
    private String iTime;

    @JsonProperty("senttime")
    private String sentTime;

    @JsonProperty("message")
    private String message;

    @JsonProperty("status")
    private String status;

    @JsonProperty("qrpath")
    private String qrPath;

    @JsonProperty("qrcode_uri")
    private String qrCodeUri;
}
