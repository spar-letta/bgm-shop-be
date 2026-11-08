package com.app.itibari.repository;

import com.app.itibari.models.entities.SmsTemplate;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, Long> {
    boolean existsByNameIgnoreCaseAndDeletedIsFalse(@NotEmpty String name);

    Optional<SmsTemplate> findByPublicIdAndDeletedIsFalse(UUID publicId);
}
