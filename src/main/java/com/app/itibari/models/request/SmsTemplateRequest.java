package com.app.itibari.models.request;

import com.app.itibari.dataTypes.SmsChannel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record SmsTemplateRequest(@NotEmpty String name, @NotNull SmsChannel smsChannel, @NotEmpty String message) {

}
