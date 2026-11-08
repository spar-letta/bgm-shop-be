package com.app.itibari.models.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record SmsTemplateResponse(String dateCreated,
                                  UUID publicId,
                                  String createdBy,
                                  String name,
                                  String channel,
                                  String message,
                                  boolean active) {
}
