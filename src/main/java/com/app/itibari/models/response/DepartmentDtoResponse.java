package com.app.itibari.models.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DepartmentDtoResponse(UUID publicId, String name) {
}
