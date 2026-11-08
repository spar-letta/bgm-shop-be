package com.app.itibari.services;

import com.app.itibari.dataTypes.NotificationChannel;
import com.app.itibari.dataTypes.ReminderJobStatus;
import com.app.itibari.dataTypes.ReminderJobType;
import com.app.itibari.dataTypes.ReminderType;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.ReminderJob;
import com.app.itibari.models.entities.ReminderLog;
import com.app.itibari.models.entities.ReminderSetting;
import com.app.itibari.models.request.ReminderSettingRequest;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.ReminderJobRepository;
import com.app.itibari.repository.ReminderLogRepository;
import com.app.itibari.repository.ReminderSettingRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ReminderSettingServiceImpl implements ReminderSettingService {

    private final ReminderSettingRepository reminderSettingRepository;
    private final PersonRepository personRepository;
    private final ReminderJobRepository reminderJobRepository;
    private final ReminderLogRepository reminderLogRepository;

    @Override
    public ReminderSetting createReminderSetting(ReminderSettingRequest request, String username) {
        Optional<Person> person = personRepository.findByEmailAndDeletedIsFalse(username);

        boolean duplicate = reminderSettingRepository
                .existsByCustomerTypeAndReminderTypeAndDeletedIsFalse(request.customerType(), request.reminderType());
        if (duplicate) {
            throw new BadRequestRestApiException()
                    .developerMessage("A reminder setting for this customer type and day already exists")
                    .userMessage("A reminder setting for this customer type and day already exists");
        }

        ReminderSetting setting = new ReminderSetting();
        setting.setCustomerType(request.customerType());
        setting.setReminderType(request.reminderType());
        setting.setPeriod(request.period());
        setting.setMessageSendTime(request.messageSendTime());
        setting.setMessageTemplate(request.messageTemplate());
        setting.setFrequency(request.frequency());
        person.ifPresent(setting::setCreatedBy);

        ReminderSetting reminderSetting = reminderSettingRepository.save(setting);

        this.createUpdateReminderJob(reminderSetting);
        return reminderSetting;
    }

    @Override
    public void updateReminderSetting(UUID publicId, ReminderSettingRequest request, String username) {
        Optional<Person> person = personRepository.findByEmailAndDeletedIsFalse(username);
        ReminderSetting setting = getReminderSetting(publicId);

        setting.setCustomerType(request.customerType());
        setting.setReminderType(request.reminderType());
        setting.setPeriod(request.period());
        setting.setMessageSendTime(request.messageSendTime());
        setting.setMessageTemplate(request.messageTemplate());
        setting.setFrequency(request.frequency());
        person.ifPresent(setting::setUpdatedBy);

        ReminderSetting updatedReminderSetting = reminderSettingRepository.save(setting);

        this.createUpdateReminderJob(updatedReminderSetting);
    }

    @Override
    public ReminderSetting getReminderSetting(UUID publicId) {
        return reminderSettingRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Reminder setting not found")
                        .userMessage("Reminder setting not found"));
    }

    @Override
    public Page<ReminderSetting> getAllReminderSettings(Pageable pageable, String username) {
        return reminderSettingRepository.findAllByDeletedIsFalse(pageable);
    }

    @Override
    public void deleteReminderSetting(UUID publicId, String username) {
        Optional<Person> person = personRepository.findByEmailAndDeletedIsFalse(username);
        ReminderSetting setting = getReminderSetting(publicId);
        setting.setDeleted(true);
        person.ifPresent(setting::setUpdatedBy);
        reminderSettingRepository.save(setting);

        List<ReminderJob> allJobs = reminderJobRepository.findAllByReminderSettingAndDeletedIsFalse(setting);
        allJobs.forEach(j -> {
            j.setDeleted(true);
            person.ifPresent(j::setUpdatedBy);
        });
        reminderJobRepository.saveAll(allJobs);
    }

    private void createUpdateReminderJob(ReminderSetting reminderSetting) {
        // Cancel all pending scheduled jobs for this setting — avoids NonUniqueResultException
        // on subsequent updates (scheduleNextOccurrence chains new jobs under the same setting)
        List<ReminderJob> scheduled = reminderJobRepository
                .findAllByReminderSettingAndStatusAndDeletedIsFalse(reminderSetting, ReminderJobStatus.SCHEDULED);
        scheduled.forEach(j -> j.setDeleted(true));
        reminderJobRepository.saveAll(scheduled);

        ReminderJob reminderJob = new ReminderJob();
        reminderJob.setReminderSetting(reminderSetting);
        reminderJob.setJobType(reminderSetting.getReminderType() == ReminderType.OVERDUE ? ReminderJobType.OVERDUE : ReminderJobType.DUE_SOON);
        reminderJob.setScheduledTime(LocalDateTime.of(LocalDate.now(), reminderSetting.getMessageSendTime()));
        reminderJob.setNotificationChannel(NotificationChannel.SMS);
        reminderJob.setStatus(ReminderJobStatus.SCHEDULED);
        reminderJobRepository.save(reminderJob);
    }

    @Override
    public Page<ReminderLog> fetchReminderLogs(String searchParam, String from, String to, Pageable pageable) {
        LocalDateTime fromDt = (from != null && !from.isBlank())
                ? LocalDate.parse(from).atStartOfDay()
                : null;
        LocalDateTime toDt = (to != null && !to.isBlank())
                ? LocalDate.parse(to).atTime(LocalTime.MAX)
                : null;
        String search = (searchParam != null && !searchParam.isBlank()) ? searchParam.trim() : null;
        return reminderLogRepository.searchLogs(search, fromDt, toDt, pageable);
    }
}
