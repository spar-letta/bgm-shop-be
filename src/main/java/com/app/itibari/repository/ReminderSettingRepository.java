package com.app.itibari.repository;

import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.dataTypes.ReminderType;
import com.app.itibari.models.entities.ReminderSetting;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReminderSettingRepository extends JpaRepository<ReminderSetting, Long> {

    Optional<ReminderSetting> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<ReminderSetting> findAllByDeletedIsFalse(Pageable pageable);

    boolean existsByCustomerTypeAndReminderTypeAndDeletedIsFalse(@NotNull(message = "customerType is required") CustomerType customerType, @NotNull(message = "reminderType is required") ReminderType reminderType);
}
