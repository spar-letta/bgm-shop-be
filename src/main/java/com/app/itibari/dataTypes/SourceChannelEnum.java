package com.app.itibari.dataTypes;

public enum SourceChannelEnum {
    CUSTOMER_CARE_APP("Customer Care App"),
    SALES_AGENTS_APP("Sales Agent App"),
    BACK_OFFICE_APP("Back Office App"),
    DELIVERY_AGENT_APP("Delivery Agent App"),
    ONE_SALES("One Sales");

    private final String label;

    SourceChannelEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}