package com.app.itibari.models.response;

import lombok.Builder;

import java.util.UUID;
@Builder
public record CustomerNotesDTO(String dateCreated,
                               String timeCreated,
                               UUID publicId,
                               String content,
                               String noteType,
                               UUID customerPublicId,
                               String customerName,
                               String reasonCode,
                               UUID reasonCodePublicId,
                               String departmentName,
                               UUID departmentPublicId,
                               String createdByName,
                               String updatedByName
) {
}
