package com.app.itibari.models.response;

import java.util.UUID;

public record CallStatusSummary(
        UUID publicId,
        String code,
        String description
) {
}
