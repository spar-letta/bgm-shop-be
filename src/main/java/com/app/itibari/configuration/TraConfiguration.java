package com.app.itibari.configuration;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
@Getter
public class TraConfiguration {
    @Value("${tra.branch}")
    private String branch;
    @Value("${tra.department}")
    private String department;
    @Value("${tra.device-number}")
    private String deviceNumber;
    @Value("${tra.customer-id-type}")
    private String customerIdType;
    @Value("${tra.pay-type}")
    private String payType;
    @Value("${tra.fcode}")
    private String fCode;
    @Value("${tra.fcode-token}")
    private String fCodeToken;
    @Value("${tra.invoice-prefix}")
    private String invoicePrefix;

}
