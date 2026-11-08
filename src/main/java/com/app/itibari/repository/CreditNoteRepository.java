package com.app.itibari.repository;

import com.app.itibari.models.entities.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditNoteRepository extends JpaRepository<CreditNote, Long>, JpaSpecificationExecutor<CreditNote> {
    List<CreditNote> findByPurchaseOrderPublicIdAndDeletedIsFalse(String string);

    Optional<CreditNote> findByPublicIdAndDeletedIsFalse(UUID creditNotePublicId);
}
