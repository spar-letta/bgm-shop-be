package com.app.itibari.models.request;

import lombok.Builder;

@Builder
public record DisposalRequest(String comment) {
}
