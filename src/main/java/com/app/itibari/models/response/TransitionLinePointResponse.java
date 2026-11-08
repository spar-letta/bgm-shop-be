package com.app.itibari.models.response;

import lombok.Builder;

import java.util.List;

@Builder
public record TransitionLinePointResponse(List<String> status, String currentStatus) {

}
