package com.app.itibari.services;

import com.app.itibari.models.entities.ReminderJob;
import com.app.itibari.repository.ReminderJobRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ReminderJobServiceImpl implements ReminderJobService {

    private final ReminderJobRepository reminderJobRepository;


    @Override
    public ReminderJob getReminderJob(UUID publicId) {
        return reminderJobRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Reminder job not found")
                        .userMessage("Reminder job not found"));
    }

    @Override
    public Page<ReminderJob> getAllReminderJobs(Pageable pageable, String username) {
        return reminderJobRepository.findAllByDeletedIsFalse(pageable);
    }
}
