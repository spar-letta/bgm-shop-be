package com.app.itibari.repository;

import com.app.itibari.models.entities.SmsGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SmsGroupRepository extends JpaRepository<SmsGroup, Long> {

    Optional<SmsGroup> findByNameIgnoreCaseAndDeletedIsFalse(String groupName);

    Optional<SmsGroup> findByPublicIdAndDeletedIsFalse(UUID uuid);
}
