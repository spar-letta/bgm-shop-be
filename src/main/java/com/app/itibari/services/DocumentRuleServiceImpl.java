package com.app.itibari.services;

import com.app.itibari.dataTypes.EntityType;
import com.app.itibari.models.entities.DocumentRule;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.TaxCode;
import com.app.itibari.models.request.CreateDocumentRuleRequest;
import com.app.itibari.models.request.UpdateDocumentRuleRequest;
import com.app.itibari.repository.DocumentRuleRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.specification.DocumentRuleSpecification;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class DocumentRuleServiceImpl implements DocumentRuleService {
    private final DocumentRuleRepository documentRuleRepository;
    private final PersonRepository personRepository;

    public DocumentRuleServiceImpl(DocumentRuleRepository documentRuleRepository, PersonRepository personRepository) {
        this.documentRuleRepository = documentRuleRepository;
        this.personRepository = personRepository;
    }

    @Override
    public DocumentRule createDocumentRule(CreateDocumentRuleRequest createDocumentRuleRequest, String name) {
        Person person = getLoggedInPerson(name);


        if (ObjectUtils.allNotNull(createDocumentRuleRequest.getEntityType(), createDocumentRuleRequest.getDocumentName(), createDocumentRuleRequest.getEntitySubType())) {
            Optional<DocumentRule> optionalDocumentRule = documentRuleRepository.findDocumentRuleByDocumentNameIgnoreCaseAndEntityTypeAndEntitySubTypeIgnoreCaseAndDeletedIsFalse(
                    createDocumentRuleRequest.getDocumentName(),
                    createDocumentRuleRequest.getEntityType(),
                    createDocumentRuleRequest.getEntitySubType()
            );
            if (optionalDocumentRule.isPresent()) {
                log.error("Document Rule with name {}, entity type {} and entity sub type {} already exists",
                        createDocumentRuleRequest.getDocumentName(),
                        createDocumentRuleRequest.getEntityType(),
                        createDocumentRuleRequest.getEntitySubType());
                throw new BadRequestRestApiException()
                        .developerMessage("Document Rule with name " + createDocumentRuleRequest.getDocumentName() +
                                ", entity type " + createDocumentRuleRequest.getEntityType() +
                                " and entity sub type " + createDocumentRuleRequest.getEntitySubType() + " already exists")
                        .userMessage("Document Rule already exists");
            }
        } else if (ObjectUtils.allNotNull(createDocumentRuleRequest.getEntityType(), createDocumentRuleRequest.getDocumentName())) {
            Optional<DocumentRule> optionalDocumentRule = documentRuleRepository.findDocumentRuleByDocumentNameIgnoreCaseAndEntityTypeAndDeletedIsFalse(
                    createDocumentRuleRequest.getDocumentName(),
                    createDocumentRuleRequest.getEntityType()
            );
            if (optionalDocumentRule.isPresent()) {
                log.error("Document Rule with name {}, entity type {} already exists",
                        createDocumentRuleRequest.getDocumentName(),
                        createDocumentRuleRequest.getEntityType());
                throw new BadRequestRestApiException()
                        .developerMessage("Document Rule with name " + createDocumentRuleRequest.getDocumentName() +
                                " and entity type " + createDocumentRuleRequest.getEntityType() + " already exists")
                        .userMessage("Document Rule already exists");
            }
        }

        DocumentRule documentRule = new DocumentRule();
        documentRule.setDocumentName(createDocumentRuleRequest.getDocumentName());
        documentRule.setEntityType(createDocumentRuleRequest.getEntityType());
        documentRule.setEntitySubType(createDocumentRuleRequest.getEntitySubType());
        documentRule.setDocumentDescription(createDocumentRuleRequest.getDescription());
        documentRule.setDocumentExpiryCheck(createDocumentRuleRequest.isDocumentExpiryCheck());
        documentRule.setDocumentRequired(createDocumentRuleRequest.isDocumentRequired());
        documentRule.setCreatedBy(person);
        documentRule.setDocumentType(createDocumentRuleRequest.getDocumentType());

        return documentRuleRepository.save(documentRule);
    }

    @Override
    public Page<DocumentRule> retrieveDocumentRule(EntityType entityType, String entitySubType, String name, Pageable pageable) {
        Specification<DocumentRule> spec = Specification
                .where(DocumentRuleSpecification.isNotDeleted())
                .and(DocumentRuleSpecification.hasEntityType(entityType))
                .and(DocumentRuleSpecification.hasEntitySubType(entitySubType));

        return documentRuleRepository.findAll(spec, pageable);
    }

    @Override
    public DocumentRule updateDocumentRule(UpdateDocumentRuleRequest updateDocumentRuleRequest, String name) {
        DocumentRule documentRule = fetchDocumentRule(updateDocumentRuleRequest.getPublicId());
        Person updatedBy = getLoggedInPerson(name);

        //check if name exists
        documentRuleRepository.findDocumentRuleByDocumentNameIgnoreCaseAndEntityTypeAndDeletedIsFalse(updateDocumentRuleRequest.getDocumentName(), updateDocumentRuleRequest.getEntityType()).ifPresent(foundDocumentRule -> {
            if (!org.springframework.util.ObjectUtils.nullSafeEquals(documentRule.getId(), foundDocumentRule.getId())) {
                throw new BadRequestRestApiException()
                        .developerMessage("Document Rule Already Defined")
                        .userMessage("Sorry. Document Rule Already Defined");
            }
        });

        if (!ObjectUtils.isEmpty(updateDocumentRuleRequest.getEntitySubType())) {
            documentRuleRepository.findDocumentRuleByDocumentNameIgnoreCaseAndEntityTypeAndEntitySubTypeIgnoreCaseAndDeletedIsFalse(updateDocumentRuleRequest.getDocumentName(), updateDocumentRuleRequest.getEntityType(), updateDocumentRuleRequest.getEntitySubType()).ifPresent(foundDocumentRule -> {
                if (!org.springframework.util.ObjectUtils.nullSafeEquals(documentRule.getId(), foundDocumentRule.getId())) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Document Rule Already Defined")
                            .userMessage("Sorry. Document Rule Already Defined");
                }
            });
        }

        documentRule.setUpdatedBy(updatedBy);
        documentRule.setDocumentName(updateDocumentRuleRequest.getDocumentName());
        documentRule.setDocumentDescription(updateDocumentRuleRequest.getDescription());
        documentRule.setEntitySubType(updateDocumentRuleRequest.getEntitySubType());
        documentRule.setDocumentRequired(updateDocumentRuleRequest.isDocumentRequired());
        documentRule.setDocumentExpiryCheck(updateDocumentRuleRequest.isDocumentExpiryCheck());
        documentRule.setUpdateDate(LocalDateTime.now());

        if (!org.springframework.util.ObjectUtils.isEmpty(updateDocumentRuleRequest.getDocumentType())) {
            documentRule.setDocumentType(updateDocumentRuleRequest.getDocumentType());
        }
        return documentRuleRepository.save(documentRule);
    }

    @Override
    public DocumentRule getDocumentRuleByPublicId(UUID documentPublicId) {
        return fetchDocumentRule(documentPublicId);
    }

    @Override
    public void deleteDocumentRule(UUID documentPublicId, String name) {
        DocumentRule documentRule = fetchDocumentRule(documentPublicId);
        Person deletedBy = getLoggedInPerson(name);

        documentRule.setDeleted(true);
        documentRule.setUpdatedBy(deletedBy);
        documentRule.setUpdateDate(LocalDateTime.now());

        documentRuleRepository.save(documentRule);
    }

    private DocumentRule fetchDocumentRule(@NotNull(message = "missing public") UUID publicId) {
        Optional<DocumentRule> documentRuleOptional = documentRuleRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (documentRuleOptional.isEmpty()) {
            log.error("Document Rule with public id {} not found", publicId);
            throw new BadRequestRestApiException()
                    .developerMessage("Document Rule with public id " + publicId + " not found")
                    .userMessage("Document Rule not found");
        }

        return documentRuleOptional.get();
    }

    private Person getLoggedInPerson(String name) {
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        if (personOptional.isEmpty()) {
            log.error("Person with email {} not found", name);
            throw new BadRequestRestApiException()
                    .developerMessage("Person with email " + name + " not found")
                    .userMessage("Logged in user not found");
        }

        return personOptional.get();
    }
}
