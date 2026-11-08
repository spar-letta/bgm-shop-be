package com.app.itibari.models.request;

import com.app.itibari.dataTypes.DocumentTypes;
import com.app.itibari.dataTypes.EntityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDocumentRuleRequest {

    @NotNull(message = "document name is missing")
    private String documentName;

    @NotNull(message = "entity type is missing")
    private EntityType entityType;

    private String entitySubType;

    private boolean documentRequired;

    @Size(max = 250)
    private String description;

    private boolean documentExpiryCheck;

    private DocumentTypes documentType;

}
