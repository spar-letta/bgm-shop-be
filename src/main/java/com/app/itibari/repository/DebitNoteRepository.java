package com.app.itibari.repository;

import com.app.itibari.dataTypes.DebitReferenceType;
import com.app.itibari.models.entities.DebitNotes;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DebitNoteRepository extends JpaRepository<DebitNotes, Long>, JpaSpecificationExecutor<DebitNotes> {

    @Query("SELECT d FROM DebitNotes d WHERE d.referenceId = ?1 AND d.referenceType = ?2 AND d.reconciled = false AND d.deleted = false")
    List<DebitNotes> findByReferenceIdAndReconciledIsFalseAndReferenceTypeAndDeletedIsFalse(UUID publicId,
                                                                                            DebitReferenceType debitReferenceType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DebitNotes d where d.publicId = :publicId and d.deleted = false")
    Optional<DebitNotes> findByPublicIdForUpdate(@Param("publicId") UUID publicId);

    Optional<DebitNotes> findByPublicIdAndDeletedIsFalse(UUID publicId);

    @Query("select count(d) > 0 from DebitNotes d " +
            "where d.referenceId = :agentId and d.referenceType = :referenceType and d.deleted = false " +
            "and (d.reconciliationPublicId = :reconciliationId or exists " +
            "(select r.id from Reconciliation r where cast(r.publicId as string) = d.reconciliationPublicId " +
            "and r.routePublicId = :routeId))")
    boolean existsForDeliveryAgentAndRoute(@Param("agentId") UUID agentId,
                                          @Param("referenceType") DebitReferenceType referenceType,
                                          @Param("reconciliationId") String reconciliationId,
                                          @Param("routeId") String routeId);
}
