package com.app.itibari.models.entities;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Builder
public record TumaSmsDto(
        @NotEmpty String messageBody,
        Set<String> multiplePhoneNumber,
        Set<String> customVariable,
        @NotNull Boolean isCustomer,
        @NotNull Boolean isSalesAgent,
        @NotNull Boolean isDeliveryAgent,
        @NotNull Boolean isGroup,
        UUID groupPublicId,
        Boolean activeCustomers,
        String campaignName,
        Boolean sms,
        Boolean whatsapp) {

    public TumaSmsDto {
        // Count how many booleans are true
        long trueCount = Stream.of(isCustomer, isSalesAgent, isDeliveryAgent, isGroup)
                .filter(Boolean.TRUE::equals)
                .count();

        if (trueCount != 1) {
            throw new BadRequestRestApiException()
                    .developerMessage("Exactly one of isCustomer, isSalesAgent, isDeliveryAgent, or isGroup must be true.")
                    .userMessage("Sorry, exactly one of isCustomer, isSalesAgent, isDeliveryAgent, or isGroup must be true.");
        }

        // clients that don't send the channel flags keep the SMS-only behaviour
        if (sms == null && whatsapp == null) {
            sms = Boolean.TRUE;
        }
        sms = Boolean.TRUE.equals(sms);
        whatsapp = Boolean.TRUE.equals(whatsapp);

        if (whatsapp && !Boolean.TRUE.equals(isGroup)) {
            throw new BadRequestRestApiException()
                    .developerMessage("whatsapp is only supported when isGroup is true.")
                    .userMessage("Sorry, WhatsApp can only be used when sending to a group.");
        }

        if (!sms && !whatsapp) {
            throw new BadRequestRestApiException()
                    .developerMessage("At least one of sms or whatsapp must be true.")
                    .userMessage("Sorry, please select SMS, WhatsApp or both.");
        }

        if (multiplePhoneNumber == null){
            multiplePhoneNumber = new HashSet<>();
        }

        if (customVariable == null){
            customVariable = new HashSet<>();
        }
    }
}
