package com.app.itibari.models.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record BnplLeadNotesResponse(String dateCreated,
                                    String timeCreated,
                                    UUID publicId,
                                    String description,
                                    String reasonCode,
                                    UUID reasonCodePublicId,
                                    String createdByName,
                                    String updatedByName,
                                    UUID leadPublicId) {
}
