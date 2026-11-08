package com.app.itibari.models.request;

import com.app.itibari.dataTypes.SlaType;
import lombok.Builder;

@Builder
public record PrioritySettingRequest(String priorityName,
                                     SlaType slaType,
                                     String slaDuration,
                                     String colorName,
                                     Integer firstTimeResponse) {
}
