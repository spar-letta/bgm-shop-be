package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.CallStatus;
import com.app.itibari.models.request.CallStatusRequest;
import com.app.itibari.services.CallStatusService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Tag(name = "Call Status", description = "The Call Status catalog API")
@RestController
@RequestMapping("/callStatuses")
public class CallStatusRestController {

    private final CallStatusService callStatusService;

    public CallStatusRestController(CallStatusService callStatusService) {
        this.callStatusService = callStatusService;
    }

    @JsonView({BaseView.CallStatusView.class})
    @PostMapping
    // @PreAuthorize("hasAuthority('CREATE_CALL_STATUS')")
    @Operation(summary = "Create a new call status", description = "Create a new call status catalog entry",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_CALL_STATUS", value = Examples.CREATE_CALL_STATUS)
    })))
    public CallStatus createCallStatus(@RequestBody @Valid CallStatusRequest request,
                                       @Parameter(hidden = true) Principal principal) {
        return callStatusService.createCallStatus(request, principal.getName());
    }

    // @PreAuthorize("hasAuthority('UPDATE_CALL_STATUS')")
    @PatchMapping("/{publicId}")
    @Operation(summary = "Update a call status", description = "Update code or description of an existing call status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_CALL_STATUS", value = Examples.UPDATE_CALL_STATUS)
    })))
    public void updateCallStatus(@PathVariable(name = "publicId") UUID publicId,
                                 @RequestBody @Valid CallStatusRequest request,
                                 @Parameter(hidden = true) Principal principal) {
        callStatusService.updateCallStatus(publicId, request, principal.getName());
    }

    // @PreAuthorize("hasAuthority('DELETE_CALL_STATUS')")
    @DeleteMapping("/{publicId}")
    @Operation(summary = "Deactivate a call status",
            description = "Soft-deactivates a call status. The row is retained for history; " +
                    "deactivated statuses no longer appear in the active options list.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deactivateCallStatus(@PathVariable(name = "publicId") UUID publicId,
                                     @Parameter(hidden = true) Principal principal) {
        callStatusService.deactivateCallStatus(publicId, principal.getName());
    }

    // @PreAuthorize("hasAuthority('UPDATE_CALL_STATUS')")
    @PatchMapping("/{publicId}/activate")
    @Operation(summary = "Reactivate a call status", description = "Reactivates a previously deactivated call status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void activateCallStatus(@PathVariable(name = "publicId") UUID publicId,
                                   @Parameter(hidden = true) Principal principal) {
        callStatusService.activateCallStatus(publicId, principal.getName());
    }

    @JsonView({BaseView.CallStatusView.class})
    @GetMapping
    // @PreAuthorize("hasAuthority('VIEW_CALL_STATUS')")
    @Operation(summary = "Get all call statuses",
            description = "Returns a paginated list of call statuses. " +
                    "Filter by active=true to retrieve only options available for agents.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<CallStatus> getCallStatuses(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "active", required = false) Boolean active,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return callStatusService.getCallStatuses(searchParam, active, pageable);
    }
}
