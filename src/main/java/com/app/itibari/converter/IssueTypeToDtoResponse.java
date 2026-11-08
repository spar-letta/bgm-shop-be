package com.app.itibari.converter;

import com.app.itibari.models.entities.IssueType;
import com.app.itibari.models.response.IssueTypeResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class IssueTypeToDtoResponse implements Converter<IssueType, IssueTypeResponse> {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE dd, MMM yyyy");

    @Override
    public IssueTypeResponse convert(IssueType issueType) {
        if (issueType == null){
            return null;
        }

        LocalDateTime creationTime = issueType.getCreationTime();
        String dateString = creationTime.toLocalDate().format(DATE_FORMATTER);

        return IssueTypeResponse.builder()
                .name(issueType.getName())
                .publicId(issueType.getPublicId())
                .departmentId(issueType.getDepartment().getPublicId())
                .departmentName(issueType.getDepartment().getName())
                .priorityName(issueType.getPrioritySetting() != null ? issueType.getPrioritySetting().getPriorityName(): null)
                .slaType(issueType.getPrioritySetting() != null ? issueType.getPrioritySetting().getSlaType().name(): null)
                .slaDuration(issueType.getPrioritySetting() != null ? issueType.getPrioritySetting().getSlaDuration(): null)
                .colorName(issueType.getPrioritySetting() != null ? issueType.getPrioritySetting().getColorName(): null)
                .priorityId(issueType.getPrioritySetting() != null ? issueType.getPrioritySetting().getPublicId(): null)
                .createdByName(issueType.getCreatedBy() != null ? issueType.getCreatedBy().getFullName(): null)
                .createdDate(dateString)
                .isActive(issueType.getIsActive())
                .customerType(issueType.getCustomerType() != null ? issueType.getCustomerType().name().replace("_", " ") : "--")
                .build();
    }
}
