package com.app.itibari.models.request;

import lombok.Builder;

@Builder
public record TestPayload(String templatePublicId, String transitionName) {
}
