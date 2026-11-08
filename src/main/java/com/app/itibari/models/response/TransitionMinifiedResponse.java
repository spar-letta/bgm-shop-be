package com.app.itibari.models.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record TransitionMinifiedResponse(List<TransitionDTO> transitionDTOList) {

    @Builder
    public record TransitionDTO(UUID publicId, String name, String departmentName, String modifiedName){}
}
