package com.app.itibari.services;

import com.app.itibari.models.entities.ReminderJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReminderJobService {

    ReminderJob getReminderJob(UUID publicId);

    Page<ReminderJob> getAllReminderJobs(Pageable pageable, String username);
}
