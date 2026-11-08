package com.app.itibari.models.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record HelpDeskCustomerDTOResponse(UUID publicId, String customerName, String phone, String storeName,
                                          String customerType, District district, Ward ward, Long ticketSize, UUID existingCustomerPublicId) {

    @Builder
    public record District(String publicId, String name){

    }

    @Builder
    public record Ward(String publicId, String name){

    }

}
