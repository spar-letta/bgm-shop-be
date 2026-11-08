package com.app.itibari.repository;

import com.app.itibari.dataTypes.NoteType;
import com.app.itibari.models.entities.BnplLead;
import com.app.itibari.models.entities.CustomerNotes;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerNotesRepository extends JpaRepository<CustomerNotes, Long> {
    Optional<CustomerNotes> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<CustomerNotes> findAllByCustomerAndDeletedIsFalse(Customers customer, Pageable pageable);

    Page<CustomerNotes> findAllByBnplLeadAndDeletedIsFalse(BnplLead lead, Pageable pageable);

    List<CustomerNotes> findByNoteTypeAndNotificationSendIsFalseAndDeletedIsFalse(NoteType noteType);

    List<CustomerNotes> findByNoteTypeAndNotificationSendIsFalseAndDeletedIsFalseAndReasonCode_Department(NoteType noteType, Department department);

    List<CustomerNotes> findByCustomerAndDeletedIsFalse(Customers customer);
}
