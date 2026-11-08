package com.app.itibari.models.request;

import com.app.itibari.dataTypes.DocumentTypes;
import com.app.itibari.dataTypes.EntityType;
import com.app.itibari.docs.Examples;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(example = Examples.UPDATE_DOCUMENT_RULE_REQUEST)
public class UpdateDocumentRuleRequest {
    @NotNull(message = "missing public")
    private UUID publicId;
    @NotNull(message = "document name is missing")
    private String documentName;
    @NotNull(message = "entity type is missing")
    private EntityType entityType;
    private String entitySubType;
    private boolean documentRequired;
    private boolean documentExpiryCheck;
    private String description;
    private DocumentTypes documentType;
}
