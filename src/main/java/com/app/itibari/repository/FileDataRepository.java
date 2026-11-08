package com.app.itibari.repository;

import com.app.itibari.models.entities.DocumentRule;
import com.app.itibari.models.entities.FileData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileDataRepository extends JpaRepository<FileData, Long>, JpaSpecificationExecutor<FileData> {

    Page<FileData> findAllByReferenceIdAndReferenceTypeAndDeletedIsFalse(String string, String name, Pageable pageable);

    Optional<FileData> findByPublicIdAndDeletedIsFalse(UUID publicId);

    @Query("SELECT f FROM FileData f WHERE f.referenceId = ?1 AND f.referenceType = ?2 AND f.receipt = ?3 AND f.deleted = false")
    List<FileData> findAllByReferenceIdAndReferenceTypeAndReceiptIgnoreCaseAndDeletedIsFalse(String string, String name, String receipt);

    @Query("SELECT f FROM FileData f WHERE f.receipt = ?1 AND f.referenceType = ?2 AND f.deleted = false")
    List<FileData> findAllByReceiptIgnoreCaseAndReferenceTypeAndDeletedIsFalse(String receipt, String name);

    @Query("SELECT f FROM FileData f WHERE f.publicId = ?1 AND f.referenceType = ?2 AND f.deleted = false")
    Optional<FileData> findByPublicIdAndReferenceTypeIgnoreCaseAndDeletedIsFalse(UUID publicId, String name);

    @Query("SELECT f FROM FileData f WHERE f.referenceId = ?1 AND f.referenceType = ?2 AND f.documentRule = ?3 AND f.deleted = false")
    List<FileData> findAllByReferenceIdAndReferenceTypeAndDocumentRuleAndDeletedIsFalse(String string, String name, DocumentRule documentRule);

    List<FileData> findByReferenceTypeAndActiveFileIsTrueAndDeletedIsFalse(String name);

    List<FileData> findByReferenceTypeAndReferenceIdAndDescriptionIgnoreCaseAndDeletedIsFalse(String referenceType, String string, String orderNumber);


    boolean existsByReceiptIgnoreCaseAndReferenceTypeAndDeletedIsFalse(String receipt, String name);

    @Query("SELECT f FROM FileData f WHERE f.referenceType = ?1 AND f.activeFile = ?2 AND f.deleted = false")
    List<FileData> findByReferenceTypeAndActiveFileAndDeletedIsFalse(String name, Boolean isActive);

    @Query("SELECT f " +
            "FROM FileData f " +
            "JOIN f.documentRule d " +
            "WHERE d.documentExpiryCheck = true " +
            "AND f.expiry = ?1 " +
            "AND f.deleted = false " +
            "AND f.fifteenDayProcessed = false")
    List<FileData> findAllFifteenDayByDocumentRuleAndDeletedIsFalse(LocalDate expiryDate);

    @Query("SELECT f " +
            "FROM FileData f " +
            "JOIN f.documentRule d " +
            "WHERE d.documentExpiryCheck = true " +
            "AND f.expiry = ?1 " +
            "AND f.deleted = false " +
            "AND f.thirtyDayProcessed = false")
    List<FileData> findAllThirtyDayByDocumentRuleAndDeletedIsFalse(LocalDate expiryDate);

    boolean existsByReceiptIgnoreCaseAndDeletedIsFalse(String receipt);
}
