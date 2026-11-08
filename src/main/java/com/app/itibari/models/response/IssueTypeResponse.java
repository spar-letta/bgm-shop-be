package com.app.itibari.models.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record IssueTypeResponse(String name, UUID publicId, String departmentName,
                                UUID departmentId, String priorityName,
                                String slaType,
                                String slaDuration,
                                String colorName,
                                UUID priorityId,
                                String createdByName,
                                String createdDate,
                                Boolean isActive,
                                String customerType) {
}
