package com.app.itibari.models.request;

import com.app.itibari.dataTypes.AssignToEnum;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record CustomerExperienceRequest(
        String templateName,
        UUID issueTypePublicId,
        List<UUID> departmentInCopyPublicId,
        AssignToEnum assignTo,
        String pocEmail,
        List<String> emailInCopy,
        List<Map<String, Object>> customFields,
        boolean allowNotification) {

    public CustomerExperienceRequest{
        if (emailInCopy == null || emailInCopy.isEmpty()){
            emailInCopy = new ArrayList<>();
        }

        if (departmentInCopyPublicId == null || departmentInCopyPublicId.isEmpty()){
            departmentInCopyPublicId = new ArrayList<>();
        }
    }
}
