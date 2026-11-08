package com.app.itibari.models.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record PersonMiniData(UUID publicId, String name) {
}
