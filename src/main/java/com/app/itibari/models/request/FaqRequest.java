package com.app.itibari.models.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record FaqRequest(@NotNull UUID departmentPublicId,
                         List<FaqInner> faqInnerList) {
    @Builder
    public record FaqInner(@NotNull UUID issueTypePublicId,
                           @NotEmpty String question,
                           @NotEmpty String answer) {
    }
}
