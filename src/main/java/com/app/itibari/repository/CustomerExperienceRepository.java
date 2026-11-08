package com.app.itibari.repository;

import com.app.itibari.models.entities.CustomerExperience;
import com.app.itibari.models.entities.IssueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerExperienceRepository extends JpaRepository<CustomerExperience, Long> {
    Page<CustomerExperience> findByIssueType(IssueType issueType, Pageable pageable);

    Optional<CustomerExperience> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<CustomerExperience> findByIssueTypeAndDeletedIsFalse(IssueType issueTypeFound);
}
