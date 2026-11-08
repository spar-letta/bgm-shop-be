package com.app.itibari.models.response;

import lombok.Builder;

@Builder
public record SmsSummaryDto(Long totalSendMessage, Long totalDelivered, Long totalFailed) {
}
