package com.app.itibari.services;

import com.app.itibari.dataTypes.EntityType;
import com.app.itibari.models.entities.DocumentRule;
import com.app.itibari.models.entities.TaxCode;
import com.app.itibari.models.request.CreateDocumentRuleRequest;
import com.app.itibari.models.request.UpdateDocumentRuleRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DocumentRuleService {
    DocumentRule createDocumentRule(@Valid CreateDocumentRuleRequest createDocumentRuleRequest, String name);

    Page<DocumentRule> retrieveDocumentRule(EntityType entityType, String entitySubType, String name, Pageable pageable);

    DocumentRule updateDocumentRule(@Valid UpdateDocumentRuleRequest updateDocumentRuleRequest, String name);

    DocumentRule getDocumentRuleByPublicId(UUID documentPublicId);

    void deleteDocumentRule(UUID documentPublicId, String name);

}
