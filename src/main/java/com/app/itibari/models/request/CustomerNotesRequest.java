package com.app.itibari.models.request;

import com.app.itibari.dataTypes.NoteType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CustomerNotesRequest(@NotEmpty String content, @NotNull NoteType noteType, UUID reasonCodePublicId) {
}
