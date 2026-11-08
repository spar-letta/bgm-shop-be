package com.app.itibari.repository;

import com.app.itibari.models.entities.BnplCallAttempt;
import com.app.itibari.models.entities.BnplLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BnplCallAttemptRepository extends JpaRepository<BnplCallAttempt, Long> {

    Optional<BnplCallAttempt> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<BnplCallAttempt> findByLeadAndDeletedIsFalseOrderByStartTimeDesc(BnplLead lead, Pageable pageable);

    List<BnplCallAttempt> findByLeadAndDeletedIsFalse(BnplLead lead);
}