package com.app.itibari.models.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record HelpDeskNotificationResponseDTO(Long count, List<DeskDto> deskDtoList) {

    @Builder
    public record DeskDto(UUID publicId,
                          String title,
                          Boolean read,
                          String createdDate,
                          String createdTime){}
}
