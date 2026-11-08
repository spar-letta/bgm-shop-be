package com.app.itibari.models.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record PrioritySettingResponse(UUID publicId,
                                      String creationTime,
                                      String updateDate,
                                      String priorityName,
                                      String slaType,
                                      String slaDuration,
                                      String colorName,
                                      Boolean isActive,
                                      String createdBy,
                                      Integer firstTimeResponse) {
}
