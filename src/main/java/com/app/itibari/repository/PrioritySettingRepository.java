package com.app.itibari.repository;

import com.app.itibari.models.entities.PrioritySetting;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PrioritySettingRepository extends JpaRepository<PrioritySetting, Long> {
    Optional<PrioritySetting> findByPriorityNameIgnoreCaseAndDeletedIsFalse(String priorityName);

    Optional<PrioritySetting> findByPublicIdAndDeletedIsFalse(@NotNull UUID uuid);

    Page<PrioritySetting> findByDeletedIsFalse(Pageable pageable);
}
