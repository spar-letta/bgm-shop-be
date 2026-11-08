package com.app.itibari.models.request;

import lombok.Builder;

import java.util.List;

@Builder
public record TransitionLineDTO(List<TransitionLine> transitionLineList) {

    @Builder
    public record TransitionLine(String from,
                                 String fromDepartment,
                                 String to,
                                 String toDepartment,
                                 String duration,
                                 Integer index) {

    }
}
