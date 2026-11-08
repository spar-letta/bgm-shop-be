package com.app.itibari.repository;

import com.app.itibari.dataTypes.DocumentTypes;
import com.app.itibari.dataTypes.EntityType;
import com.app.itibari.models.entities.DocumentRule;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRuleRepository extends JpaRepository<DocumentRule, Long>, JpaSpecificationExecutor<DocumentRule> {
    Optional<DocumentRule> findDocumentRuleByDocumentNameIgnoreCaseAndEntityTypeAndEntitySubTypeIgnoreCaseAndDeletedIsFalse(@NotNull(message = "document name is missing") String documentName, @NotNull(message = "entity type is missing") EntityType entityType, String entitySubType);

    Optional<DocumentRule> findDocumentRuleByDocumentNameIgnoreCaseAndEntityTypeAndDeletedIsFalse(@NotNull(message = "document name is missing") String documentName, @NotNull(message = "entity type is missing") EntityType entityType);

    Optional<DocumentRule> findByPublicIdAndDeletedIsFalse(@NotNull(message = "missing public") UUID publicId);

    List<DocumentRule> findDocumentRuleByDocumentTypeAndDocumentRequiredIsTrueAndDeletedIsFalse(DocumentTypes documentTypes);

    List<DocumentRule> findAllByDocumentExpiryCheckIsTrueAndDeletedIsFalse();
}
