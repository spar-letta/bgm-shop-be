package com.app.itibari.converter;

import com.app.itibari.models.entities.CustomerNotes;
import com.app.itibari.models.response.CustomerNotesDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class CustomerNotesToResponse implements Converter<CustomerNotes, CustomerNotesDTO> {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE dd, MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    @Override
    public CustomerNotesDTO convert(CustomerNotes customerNotes) {
        if (customerNotes == null) {
            return null;
        }

        LocalDateTime creationTime = customerNotes.getCreationTime();
        String dateString = creationTime.toLocalDate().format(DATE_FORMATTER);
        String timeString = creationTime.format(TIME_FORMATTER);
        String departmentName = "--";
        UUID departmentPublicId = null;
        if (customerNotes.getReasonCode() != null && customerNotes.getReasonCode().getDepartment() != null){
            departmentName = customerNotes.getReasonCode().getDepartment().getName();
            departmentPublicId = customerNotes.getReasonCode().getDepartment().getPublicId();
        }

        return CustomerNotesDTO.builder()
                .dateCreated(dateString)
                .timeCreated(timeString)
                .publicId(customerNotes.getPublicId())
                .content(customerNotes.getContent())
                .noteType(customerNotes.getNoteType().name())
                .customerPublicId(customerNotes.getCustomer() != null ? customerNotes.getCustomer().getPublicId(): null)
                .customerName(customerNotes.getCustomer() != null ? customerNotes.getCustomer().getFullName() : null)
                .reasonCodePublicId(customerNotes.getReasonCode() != null ? customerNotes.getReasonCode().getPublicId() : null)
                .reasonCode(customerNotes.getReasonCode() != null ? customerNotes.getReasonCode().getCode() : null)
                .departmentName(departmentName)
                .departmentPublicId(departmentPublicId)
                .createdByName(customerNotes.getCreatedBy() != null ? customerNotes.getCreatedBy().getFullName() : "--")
                .updatedByName(customerNotes.getUpdatedBy() != null ? customerNotes.getUpdatedBy().getFullName() : "--")
                .build();
    }
}
