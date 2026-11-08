package com.app.itibari.services;

import com.app.itibari.models.entities.ReminderLog;
import com.app.itibari.models.entities.ReminderSetting;
import com.app.itibari.models.request.ReminderSettingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReminderSettingService {

    ReminderSetting createReminderSetting(ReminderSettingRequest request, String username);

    void updateReminderSetting(UUID publicId, ReminderSettingRequest request, String username);

    ReminderSetting getReminderSetting(UUID publicId);

    Page<ReminderSetting> getAllReminderSettings(Pageable pageable, String username);

    void deleteReminderSetting(UUID publicId, String username);

    Page<ReminderLog> fetchReminderLogs(String searchParam, String from, String to, Pageable pageable);
}
