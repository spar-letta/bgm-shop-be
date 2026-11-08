package com.app.itibari.services;

import com.app.itibari.models.request.IssueTypeRequest;
import com.app.itibari.models.request.TicketDto;
import com.app.itibari.models.response.IssueTypeResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.util.UUID;

public interface IssueTypeService {
    IssueTypeResponse createIssueType(@Valid IssueTypeRequest issueTypeRequest, Principal principal);

    Page<IssueTypeResponse> listIssueType(UUID departmentId, String searchParam, Boolean isActive, TicketDto.CustomerType customerType, Pageable pageable);

    IssueTypeResponse getIssueType(UUID publicId);

    IssueTypeResponse updateIssueType(UUID publicId, @Valid IssueTypeRequest issueTypeRequest, Principal principal);

    IssueTypeResponse runActionOnIssueType(UUID publicId, Boolean isActive, Principal principal);

    void runDeleteIssueType(UUID publicId, Principal principal);
}
