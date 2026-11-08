package com.app.itibari.models.request;

import com.app.itibari.dataTypes.NotificationChannel;
import com.app.itibari.dataTypes.ReminderJobType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReminderJobRequest(

        @NotNull(message = "jobType is required")
        ReminderJobType jobType,

        @NotNull(message = "scheduledTime is required")
        @Future(message = "scheduledTime must be in the future")
        LocalDateTime scheduledTime,

        @NotNull(message = "notificationChannel is required")
        NotificationChannel notificationChannel,

        // optional: link to a reminder setting for message template + customer type filtering
        UUID reminderSettingPublicId
) {}