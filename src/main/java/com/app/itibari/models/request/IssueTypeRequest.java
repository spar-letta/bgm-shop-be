package com.app.itibari.models.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record IssueTypeRequest(@NotEmpty String name,
                               @NotNull UUID departmentPublicId,
                               @NotNull UUID priorityPublicId,
                               @NotNull TicketDto.CustomerType customerType) {
}
