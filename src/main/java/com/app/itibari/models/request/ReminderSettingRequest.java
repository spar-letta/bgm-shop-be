package com.app.itibari.models.request;

import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.dataTypes.ReminderFrequency;
import com.app.itibari.dataTypes.ReminderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ReminderSettingRequest(

        @NotNull(message = "customerType is required")
        CustomerType customerType,

        @NotNull(message = "reminderType is required")
        ReminderType reminderType,

        @NotNull(message = "period is required")
        @Min(value = 1, message = "period must be at least 1")
        Integer period,

        @NotNull(message = "messageSendTime is required")
        LocalTime messageSendTime,

        /**
         * Message template supporting the following placeholders:
         * {{customerName}}, {{invoiceNumber}}, {{dueDate}},
         * {{daysOverdue}}, {{amountDue}}, {{companyName}}
         */
        @NotBlank(message = "messageTemplate is required")
        String messageTemplate,

        @NotNull(message = "frequency is required")
        ReminderFrequency frequency
) {}
