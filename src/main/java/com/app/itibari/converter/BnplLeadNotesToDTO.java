package com.app.itibari.converter;

import com.app.itibari.models.entities.CustomerNotes;
import com.app.itibari.models.response.BnplLeadNotesResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class BnplLeadNotesToDTO implements Converter<CustomerNotes, BnplLeadNotesResponse> {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE dd, MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");


    @Override
    public BnplLeadNotesResponse convert(CustomerNotes customerNotes) {
        if (customerNotes == null) {
            return null;
        }

        LocalDateTime creationTime = customerNotes.getCreationTime();
        String dateString = creationTime.toLocalDate().format(DATE_FORMATTER);
        String timeString = creationTime.format(TIME_FORMATTER);


        return BnplLeadNotesResponse.builder()
                .dateCreated(dateString)
                .timeCreated(timeString)
                .publicId(customerNotes.getPublicId())
                .description(customerNotes.getReasonCode() != null ? customerNotes.getReasonCode().getDescription() : "--")
                .reasonCodePublicId(customerNotes.getReasonCode() != null ? customerNotes.getReasonCode().getPublicId() : null)
                .reasonCode(customerNotes.getReasonCode() != null ? customerNotes.getReasonCode().getCode() : null)
                .createdByName(customerNotes.getCreatedBy() != null ? customerNotes.getCreatedBy().getFullName() : "--")
                .updatedByName(customerNotes.getUpdatedBy() != null ? customerNotes.getUpdatedBy().getFullName() : "--")
                .leadPublicId(customerNotes.getBnplLead() != null ? customerNotes.getBnplLead().getPublicId() : null)
                .build();
    }
}
