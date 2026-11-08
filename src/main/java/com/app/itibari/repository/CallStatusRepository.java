package com.app.itibari.repository;

import com.app.itibari.models.entities.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CallStatusRepository extends JpaRepository<CallStatus, Long>, JpaSpecificationExecutor<CallStatus> {

    Optional<CallStatus> findByCodeIgnoreCaseAndDeletedIsFalse(String code);

    Optional<CallStatus> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<CallStatus> findByPublicIdAndActiveIsTrueAndDeletedIsFalse(UUID publicId);

    Optional<CallStatus> findByCodeIgnoreCaseAndActiveIsTrueAndDeletedIsFalse(String code);
}
