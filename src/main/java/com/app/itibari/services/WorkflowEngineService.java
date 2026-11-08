package com.app.itibari.services;

import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.TransitionDto;
import com.app.itibari.models.response.TransitionLinePointResponse;
import com.app.itibari.models.response.TransitionMinifiedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface WorkflowEngineService {
    Workflow addWorkflow(WorkflowRequest workflowRequest, Principal principal);

    ResponseEntity<List<Workflow>> fetchWorkflows();

    void executeTransition(UUID ticketId, UUID transitionPublicId, Principal principal);

    ResponseEntity<Page<Workflow>> listWorkflows(String searchParam, LocalDate startDate, LocalDate endDate, Principal principal, UUID templatePublicId, UUID issueTypePublicId, Pageable pageable);

    void addTicketStatus(UUID templatePublicId, @Valid StatusRequest statusRequest, UUID departmentPublicId, Principal principal);

    Page<WorkflowStatus> listWorkflowStatuses(UUID templatePublicId, String searchParam, UUID departmentPublicId, Principal principal, Pageable pageable);

    Page<WorkflowStatus> listAllWorkflowStatuses(UUID templatePublicId, String searchParam, UUID departmentPublicId, Principal principal, Pageable pageable);

    List<Transition> listTransitions(UUID ticketPublicId, Principal principal);

    void actionDeleteTicketStatus(UUID publicId, Principal principal);

    void actionDeleteWorkflow(UUID publicId, Principal principal);

    void updateTicketStatus(UUID statusPublicId, UUID templatePublicId, @Valid StatusUpdateRequest statusUpdateRequest, Principal principal);

    void actionDeleteTransition(UUID transitionPublicId, Principal principal);

    Workflow updateWorkflowStep(UUID workflowPublicId, @Valid AddWorkflowRequest workflowRequest, Principal principal);

    TransitionMinifiedResponse getTransitionMinifiedResponse(UUID ticketPublicId, Principal principal);

    TransitionLineDTO getTransitionLinePointResponse(UUID ticketPublicId, Principal principal);

    Workflow addDefineWorkflow(UUID workflowPublicId, @Valid AddWorkflowRequest addWorkflowRequest, Principal principal);

    Page<Department> getDepartmentLists(UUID templatePublicId, UUID ticketPublicId, Principal principal, Pageable pageable);

    void hitReverseTransitionLinePointResponse(UUID ticketPublicId, Principal principal);

    List<Map<String, Object>> ticketSlaPerDepartment(UUID ticketPublicId, Principal principal);

    void reverseToDepartment(UUID ticketPublicId, ReverseRequest request, Principal principal);

    Page<Department> getDepartments(UUID templatePublicId, Principal principal, Pageable pageable);
}
