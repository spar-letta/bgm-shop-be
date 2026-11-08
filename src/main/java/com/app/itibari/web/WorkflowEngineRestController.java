package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.CustomerFAQToResponse;
import com.app.itibari.models.response.TransitionLinePointResponse;
import com.app.itibari.models.response.TransitionMinifiedResponse;
import com.app.itibari.services.WorkflowEngineService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "WorkflowEngine", description = "WorkflowEngine API")
@RestController
@RequestMapping("/workflowEngine")
@RequiredArgsConstructor
public class WorkflowEngineRestController {

    private final WorkflowEngineService workflowEngineService;

    @JsonView(BaseView.WorkflowView.class)
    @PostMapping("/define")
//    @PreAuthorize("hasAuthority('DEFINE_WORKFLOW')")
    @Operation(summary = "define workflow", description = "define workflow", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "DEFINE_NEW_WORKFLOW", value = Examples.WORKFLOW_request),})))
    public ResponseEntity<Workflow> defineWorkflow(@Valid @RequestBody WorkflowRequest workflowRequest,
                                                   @Parameter(hidden = true) Principal principal) {

        return ResponseEntity.ok(workflowEngineService.addWorkflow(workflowRequest, principal));
    }

    @JsonView(BaseView.WorkflowView.class)
    @PostMapping("/addDefine/{workflowPublicId}")
//    @PreAuthorize("hasAuthority('ADD_DEFINE_WORKFLOW')")
    @Operation(summary = "add define workflow", description = "add define workflow", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "ADD_DEFINE_WORKFLOW", value = Examples.ADD_DEFINITION_TO_WORKFLOW_request),})))
    public ResponseEntity<Workflow> addDefineWorkflow(@PathVariable UUID workflowPublicId,
                                                      @Valid @RequestBody AddWorkflowRequest addWorkflowRequest,
                                                      @Parameter(hidden = true) Principal principal) {

        return ResponseEntity.ok(workflowEngineService.addDefineWorkflow(workflowPublicId, addWorkflowRequest, principal));
    }

    @PutMapping("/define/{workflowPublicId}/update")
//    @PreAuthorize("hasAuthority('DEFINE_WORKFLOW')")
    @Operation(summary = "update workflow step", description = "update workflow step", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_WORKFLOW", value = Examples.UPDATE_WORKFLOW_request),})))
    public Workflow defineWorkflow(@PathVariable UUID workflowPublicId,
                               @Valid @RequestBody AddWorkflowRequest workflowRequest,
                               @Parameter(hidden = true) Principal principal) {

        return workflowEngineService.updateWorkflowStep(workflowPublicId, workflowRequest, principal);
    }

    @JsonView(BaseView.WorkflowView.class)
    @GetMapping("/define")
    @Operation(summary = "Get all workflows", description = "Get all workflows",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Page<Workflow>> listWorkflows(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                        @RequestParam(name = "templatePublicId", required = false) UUID templatePublicId,
                                                        @RequestParam(name = "issueTypePublicId", required = false) UUID issueTypePublicId,
                                                        @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                        @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                        @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
                                                        @Parameter(hidden = true) Principal principal) {

        return workflowEngineService.listWorkflows(searchParam,
                startDate, endDate, principal, templatePublicId, issueTypePublicId, pageable);
    }

    @PostMapping("/ticket/{ticketId}/transition/{transitionPublicId}")
    public void transitionTicket(@PathVariable UUID ticketId,
                                 @PathVariable UUID transitionPublicId,
                                 @Parameter(hidden = true) Principal principal) {
        workflowEngineService.executeTransition(ticketId, transitionPublicId, principal);
    }

    @PostMapping("/addTicketStatus/{templatePublicId}")
    @Operation(summary = "add status", description = "add status", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "ADD_STATUS_request", value = Examples.ADD_STATUS_request),})))
    public void addTicketStatus(@PathVariable UUID templatePublicId,
                                @RequestParam(name = "departmentPublicId") UUID departmentPublicId,
                                @Valid @RequestBody StatusRequest statusRequest,
                                @Parameter(hidden = true) Principal principal) {
        workflowEngineService.addTicketStatus(templatePublicId, statusRequest, departmentPublicId, principal);
    }

    @PutMapping("/updateTicketStatus/{statusPublicId}/{templatePublicId}")
    @Operation(summary = "update status", description = "update status", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UODATE_STATUS_request", value = Examples.UPDATE_STATUS_request),})))
    public void updateTicketStatus(@PathVariable UUID statusPublicId,
                                   @PathVariable UUID templatePublicId,
                                   @Valid @RequestBody StatusUpdateRequest statusUpdateRequest,
                                   @Parameter(hidden = true) Principal principal) {
        workflowEngineService.updateTicketStatus(statusPublicId, templatePublicId, statusUpdateRequest, principal);
    }

    @GetMapping("/ticketStatusLists/{templatePublicId}")
    @JsonView(BaseView.WorkflowView.class)
    @Operation(summary = "", description = "",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<WorkflowStatus> listWorkflowStatuses(
            @PathVariable UUID templatePublicId,
            @RequestParam(name = "departmentPublicId", required = false) UUID departmentPublicId,
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {

        return workflowEngineService.listWorkflowStatuses(templatePublicId, searchParam, departmentPublicId, principal, pageable);
    }

    @GetMapping("/ticketStatusLists")
    @JsonView(BaseView.WorkflowView.class)
    @Operation(summary = "", description = "",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<WorkflowStatus> listAllWorkflowStatuses(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "templatePublicId", required = false) UUID templatePublicId,
            @RequestParam(name = "departmentPublicId", required = false) UUID departmentPublicId,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {

        return workflowEngineService.listAllWorkflowStatuses(templatePublicId, searchParam, departmentPublicId, principal, pageable);
    }

    @GetMapping("/{ticketPublicId}/transtions")
    @JsonView(BaseView.WorkflowView.class)
    @Operation(summary = "", description = "",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<Transition> listTransitions(
            @PathVariable UUID ticketPublicId,
            @Parameter(hidden = true) Principal principal) {

        return workflowEngineService.listTransitions(ticketPublicId, principal);
    }

    @DeleteMapping("/{publicId}/define")
//    @PreAuthorize("hasAuthority('DELETE_TYPE')")
    @Operation(summary = "action delete workflow", description = "action delete workflow", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action delete workflow", value = ""),})))
    public void actionDeleteWorkflow(@PathVariable UUID publicId,
                                     @Parameter(hidden = true) Principal principal) {

        workflowEngineService.actionDeleteWorkflow(publicId, principal);
    }

    @DeleteMapping("/transtions/{transitionPublicId}/updateStep")
//    @PreAuthorize("hasAuthority('DELETE_TYPE')")
    @Operation(summary = "action delete transition", description = "action delete transition", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action delete transition", value = ""),})))
    public void actionDeleteTransition(@PathVariable UUID transitionPublicId,
                                       @Parameter(hidden = true) Principal principal) {

        workflowEngineService.actionDeleteTransition(transitionPublicId, principal);
    }

    @DeleteMapping("/ticketStatus/{publicId}")
//    @PreAuthorize("hasAuthority('DELETE_TYPE')")
    @Operation(summary = "action delete ticketStatus", description = "action delete ticketStatus", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action delete ticketStatus", value = ""),})))
    public void actionDeleteTicketStatus(@PathVariable UUID publicId,
                                         @Parameter(hidden = true) Principal principal) {

        workflowEngineService.actionDeleteTicketStatus(publicId, principal);
    }

    @GetMapping("/{ticketPublicId}/transtionMinified")
    @Operation(summary = "", description = "",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public TransitionMinifiedResponse getTransitionMinifiedResponse(
            @PathVariable UUID ticketPublicId,
            @Parameter(hidden = true) Principal principal) {

        return workflowEngineService.getTransitionMinifiedResponse(ticketPublicId, principal);
    }

    @GetMapping("/{ticketPublicId}/transtionLinePoints")
    @Operation(summary = "", description = "",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public TransitionLineDTO getTransitionLinePointResponse(
            @PathVariable UUID ticketPublicId,
            @Parameter(hidden = true) Principal principal) {

        return workflowEngineService.getTransitionLinePointResponse(ticketPublicId, principal);
    }

    @GetMapping("/departmentLists/{templatePublicId}")
    @JsonView(BaseView.WorkflowView.class)
    @Operation(summary = "", description = "",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<Department> getDepartmentLists(
            @PathVariable UUID templatePublicId,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {

        return workflowEngineService.getDepartments(templatePublicId, principal, pageable);
    }

    @GetMapping("/{ticketPublicId}/departmentLists/{templatePublicId}")
    @JsonView(BaseView.WorkflowView.class)
    @Operation(summary = "", description = "",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<Department> getDepartmentLists(
            @PathVariable UUID templatePublicId,
            @PathVariable UUID ticketPublicId,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {

        return workflowEngineService.getDepartmentLists(templatePublicId, ticketPublicId, principal, pageable);
    }

    @PostMapping("/{ticketPublicId}/reverseTranstionLinePoints")
    @Operation(summary = "", description = "",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public void reverseTransitionLinePointResponse(
            @PathVariable UUID ticketPublicId,
            @Parameter(hidden = true) Principal principal) {

        workflowEngineService.hitReverseTransitionLinePointResponse(ticketPublicId, principal);
    }

    @PostMapping("/{ticketPublicId}/reverseToDepartment")
    @Operation(summary = "", description = "",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public void reverseToDepartment(
            @PathVariable UUID ticketPublicId,
            @Valid @RequestBody ReverseRequest request,
            @Parameter(hidden = true) Principal principal) {

        workflowEngineService.reverseToDepartment(ticketPublicId, request, principal);
    }

    @GetMapping("/{ticketPublicId}/ticketSlaPerDepartment")
    @Operation(summary = "Get Ticket SLA Per Department", description = "Get Ticket SLA Per Department",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<Map<String, Object>> ticketSlaPerDepartment(
            @PathVariable UUID ticketPublicId,
            @Parameter(hidden = true) Principal principal) {

        return workflowEngineService.ticketSlaPerDepartment(ticketPublicId, principal);
    }
}
