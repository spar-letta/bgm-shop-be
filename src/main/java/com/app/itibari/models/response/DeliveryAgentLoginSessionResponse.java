package com.app.itibari.models.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryAgentLoginSessionResponse(
        UUID sessionPublicId, UUID deliveryAgentPublicId, String deliveryAgentName, String phoneNumber,
        String deviceId, String deviceName, String deviceModel, String platform, String appVersion,
        LocalDateTime loginTime, LocalDateTime lastActivityTime, LocalDateTime logoutTime,
        LocalDateTime expiresAt, boolean active, String logoutReason) {
}
