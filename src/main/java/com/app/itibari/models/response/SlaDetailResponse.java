package com.app.itibari.models.response;

import lombok.Builder;

@Builder
public record SlaDetailResponse(String creationDate,
                                String resolveDate,
                                String firstResponse,
                                String timeToResolution,
                                String timeRemaining,
                                String departmentArrivalTime,
                                String departmentDuration,
                                String departmentTimeRemaining) {
}
