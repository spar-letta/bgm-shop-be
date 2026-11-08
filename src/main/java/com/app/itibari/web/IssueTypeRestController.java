package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.request.IssueTypeRequest;
import com.app.itibari.models.request.TicketDto;
import com.app.itibari.models.response.IssueTypeResponse;
import com.app.itibari.services.IssueTypeService;
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
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Tag(name = "IssueType", description = "IssueType API")
@RestController
@RequestMapping("/issueTypes")
@RequiredArgsConstructor
public class IssueTypeRestController {

    private final IssueTypeService issueTypeService;

    @PostMapping
//    @PreAuthorize("hasAuthority('CREATE_ISSUE_TYPE')")
    @Operation(summary = "create issue type", description = "create issue type", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action CREATE_ISSUE_TYPE", value = Examples.CREATE_ISSUE_TYPE_request),})))
    public IssueTypeResponse createIssueType(@Valid @RequestBody IssueTypeRequest issueTypeRequest,
                                             @Parameter(hidden = true) Principal principal) {

        return issueTypeService.createIssueType(issueTypeRequest, principal);
    }

    @GetMapping
    @Operation(summary = "Get all IssueType", description = "Get all IssueType",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<IssueTypeResponse> getIssueType(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                @RequestParam(name = "departmentId", required = false) UUID departmentId,
                                                @RequestParam(name = "isActive", required = false) Boolean isActive,
                                                @RequestParam(name = "customerType", required = false) TicketDto.CustomerType customerType,
                                                @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return issueTypeService.listIssueType(departmentId, searchParam, isActive, customerType, pageable);
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get IssueType", description = "Get IssueType",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public IssueTypeResponse getIssueType(@PathVariable(required = false) UUID publicId) {

        return issueTypeService.getIssueType(publicId);
    }

    @PutMapping("/{publicId}")
//    @PreAuthorize("hasAuthority('CREATE_ISSUE_TYPE')")
    @Operation(summary = "update issue type", description = "update issue type", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action CREATE_ISSUE_TYPE", value = Examples.UPDATE_ISSUE_TYPE_request),})))
    public IssueTypeResponse updateIssueType(@PathVariable(name = "publicId") UUID publicId,
                                             @Valid @RequestBody IssueTypeRequest issueTypeRequest,
                                             @Parameter(hidden = true) Principal principal) {

        return issueTypeService.updateIssueType(publicId, issueTypeRequest, principal);
    }

    @PatchMapping("/{publicId}")
//    @PreAuthorize("hasAuthority('CREATE_ISSUE_TYPE')")
    @Operation(summary = "action on issue type", description = "action on issue type", responses = {
            @ApiResponse(responseCode = "200", description = "OK")
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Action ON ISSUE_TYPE", value = Examples.CREATE_ISSUE_TYPE_request),})))
    public IssueTypeResponse actionIssueType(@PathVariable(name = "publicId") UUID publicId,
                                             @RequestParam(name = "isActive") Boolean isActive,
                                             @Parameter(hidden = true) Principal principal) {

        return issueTypeService.runActionOnIssueType(publicId, isActive, principal);
    }

    @DeleteMapping("/{publicId}")
//    @PreAuthorize("hasAuthority('CREATE_ISSUE_TYPE')")
    @Operation(summary = "delete issue type", description = "delete issue type", responses = {
            @ApiResponse(responseCode = "200", description = "OK")})
    public void deleteIssueType(@PathVariable(name = "publicId") UUID publicId,
                                @Parameter(hidden = true) Principal principal) {

        issueTypeService.runDeleteIssueType(publicId, principal);
    }
}
