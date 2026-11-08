package com.app.itibari.models.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record SmsDefaultGroupResponse(UUID publicId, String name, String phoneNumber) {
}
