package com.app.itibari.repository;

import com.app.itibari.dataTypes.ReminderJobStatus;
import com.app.itibari.models.entities.ReminderJob;
import com.app.itibari.models.entities.ReminderSetting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReminderJobRepository extends JpaRepository<ReminderJob, Long> {

    Optional<ReminderJob> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<ReminderJob> findAllByDeletedIsFalse(Pageable pageable);

    List<ReminderJob> findAllByStatusAndScheduledTimeLessThanEqualAndDeletedIsFalse(
            ReminderJobStatus status, LocalDateTime scheduledTime);

    Optional<ReminderJob> findByReminderSettingAndDeletedIsFalse(ReminderSetting reminderSetting);

    List<ReminderJob> findAllByReminderSettingAndDeletedIsFalse(ReminderSetting reminderSetting);

    List<ReminderJob> findAllByReminderSettingAndStatusAndDeletedIsFalse(ReminderSetting reminderSetting, ReminderJobStatus status);
}
