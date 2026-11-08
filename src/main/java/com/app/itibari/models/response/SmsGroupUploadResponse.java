package com.app.itibari.models.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record SmsGroupUploadResponse(UUID groupPublicId,
                                     String groupName,
                                     int totalRows,
                                     int addedCount,
                                     int duplicateCount,
                                     int failedCount,
                                     List<RowError> errors) {

    public record RowError(int row, String error) {
    }
}
