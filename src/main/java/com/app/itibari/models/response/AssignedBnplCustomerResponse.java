package com.app.itibari.models.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AssignedBnplCustomerResponse(UUID publicId,
                                           String name,
                                           String telephone,
                                           String status,
                                           String scheduledDate,
                                           String location,
                                           String ward) {
}
