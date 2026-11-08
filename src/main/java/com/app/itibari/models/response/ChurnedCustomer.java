package com.app.itibari.models.response;

import com.app.itibari.dataTypes.ChurnedCustomerLeadStatus;
import com.app.itibari.dataTypes.CustomerType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record ChurnedCustomer(
        UUID publicId,
        String name,
        String phoneNumber,

        @JsonFormat(pattern = "MM-dd-yyyy")
        LocalDateTime lastOrderDate,

        String assignedSalesAgent,

        @JsonFormat(pattern = "MM-dd-yyyy")
        LocalDateTime lastContactDate,

        String district,
        String ward,
        CustomerType customerType,
        CallStatusSummary callStatus,
        ChurnedCustomerLeadStatus leadStatus,
        String storeName,
        String firstName,
        String lastName,
        List<NoteSummary> noteSummary,
        String ownerPhoneNumber,
        String shopKeeperNumber,
        String caller,
        @JsonFormat(pattern = "MM-dd-yyyy") LocalDateTime previousOrderDate,
        @JsonFormat(pattern = "MM-dd-yyyy") LocalDate followUpDate) {

}
