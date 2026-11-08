package com.app.itibari.models.response;

import lombok.Builder;

import java.util.UUID;
@Builder
public record FollowUpDto(UUID publicId, String time, String date, String createdBy) {
}
