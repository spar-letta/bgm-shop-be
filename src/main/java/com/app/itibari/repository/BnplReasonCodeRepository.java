package com.app.itibari.repository;

import com.app.itibari.models.entities.BnplReasonCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BnplReasonCodeRepository extends JpaRepository<BnplReasonCode, Long> {

    List<BnplReasonCode> findByIsActiveTrueAndDeletedIsFalse();

    Optional<BnplReasonCode> findByPublicIdAndDeletedIsFalse(UUID publicId);

    List<BnplReasonCode> findByPublicIdInAndDeletedIsFalse(List<UUID> publicIds);
}