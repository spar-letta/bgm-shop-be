package com.app.itibari.repository;

import com.app.itibari.models.entities.CustomerFAQNote;
import com.app.itibari.models.entities.CustomerFAQ;
import com.app.itibari.models.entities.IssueType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerFAQNoteRepository extends JpaRepository<CustomerFAQNote, Long> {
    Optional<CustomerFAQNote> findByCustomerFAQAndIssueTypeAndDeletedIsFalse(CustomerFAQ customerFAQ, IssueType issueType);

    Optional<CustomerFAQNote> findByPublicIdAndDeletedIsFalse(UUID publicId);
}
