package com.app.itibari.web;

import com.app.itibari.dataTypes.Module;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.RejectionsCode;
import com.app.itibari.models.request.RejectionCodeRequest;
import com.app.itibari.services.RejectionCodeService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Tag(name = "Rejection Code", description = "The Rejection Code API")
@RestController
@RequestMapping("/rejectionCodes")
public class RejectionCodeRestController {

    private final RejectionCodeService rejectionCodeService;

    public RejectionCodeRestController(RejectionCodeService rejectionCodeService) {

        this.rejectionCodeService = rejectionCodeService;
    }

    @JsonView({BaseView.RejectionCodeView.class})
    @PostMapping
   // @PreAuthorize("hasAuthority('CREATE_REJECTION_CODE')")
    @Operation(summary = "Create a new rejection code", description = "Create a new rejection code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_REJECTION_CODE", value = Examples.CREATE_REJECTION_CODE),
    })))
    public RejectionsCode createRejectionCode(@RequestBody RejectionCodeRequest rejectionCodeRequest,
                                              @Parameter(hidden = true) Principal principal) {

        return rejectionCodeService.createRejectionCode(rejectionCodeRequest, principal.getName());
    }

   // @PreAuthorize("hasAuthority('UPDATE_REJECTION_CODE')")
    @PatchMapping("/{publicId}")
    @Operation(summary = "Update a rejection code", description = "Update a rejection code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_REJECTION_CODE", value = Examples.CREATE_REJECTION_CODE),
    })))
    public void updateRejectionCode(@PathVariable(name = "publicId") UUID publicId,
                                    @RequestBody RejectionCodeRequest rejectionCodeRequest,
                                    @Parameter(hidden = true) Principal principal) {

        rejectionCodeService.updateRejectionCode(publicId, rejectionCodeRequest, principal.getName());
    }

    //@PreAuthorize("hasAuthority('DELETE_REJECTION_CODE')")
    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete a rejection code", description = "Delete a rejection code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteRejectionCode(@PathVariable(name = "publicId") UUID publicId,
                                    @Parameter(hidden = true) Principal principal) {

        rejectionCodeService.deleteRejectionCode(publicId, principal.getName());
    }

    //get

   // @PreAuthorize("hasAuthority('VIEW_REJECTION_CODE')")
    @JsonView({BaseView.RejectionCodeView.class})
    @GetMapping
    @Operation(summary = "Get all rejection codes", description = "Get all rejection codes",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<RejectionsCode> rejectionsCodes(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "module", required = false) Module module,
            @RequestParam(name = "departmentPublicId", required = false) UUID  departmentPublicId,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return rejectionCodeService.getRejectionCodes(searchParam,module, departmentPublicId,pageable);
    }
}
