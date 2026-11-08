package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.OrganizationRegions;
import com.app.itibari.models.request.OrganizationRegionsRequest;
import com.app.itibari.services.OrganizationRegionsService;
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

@Tag(name = "OrganizationRegions", description = "Organization Regions API")
@RestController
@RequestMapping("/organizationRegions")
public class OrganizationRegionsController {

    private final OrganizationRegionsService organizationRegionsService;

    public OrganizationRegionsController(OrganizationRegionsService organizationRegionsService) {

        this.organizationRegionsService = organizationRegionsService;
    }

    @JsonView(BaseView.OrganizationRegionsView.class)
    @PostMapping
    @Operation(summary = "Create a new Organization Region", description = "Create a new Organization Region",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_ORGANIZATION_REGION", value = Examples.CREATE_ORGANIZATION_REGION),
    })))
    public OrganizationRegions createOrganizationRegion(@RequestBody @Valid OrganizationRegionsRequest organizationRegionsRequest,
                                                        @Parameter(hidden = true) Principal principal) {

        return organizationRegionsService.createOrganizationRegion(organizationRegionsRequest, principal.getName());
    }

    @PatchMapping("/{organizationRegionPublicId}")
    @Operation(summary = "Update an Organization Region", description = "Update an Organization Region",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_ORGANIZATION_REGION", value = Examples.CREATE_ORGANIZATION_REGION),
    })))
    public void updateOrganizationRegion(@PathVariable UUID organizationRegionPublicId,
                                         @RequestBody @Valid OrganizationRegionsRequest organizationRegionsRequest,
                                         @Parameter(hidden = true) Principal principal) {

        organizationRegionsService.updateOrganizationRegion(organizationRegionPublicId, organizationRegionsRequest, principal.getName());
    }

    @JsonView(BaseView.OrganizationRegionsView.class)
    @GetMapping("/{organizationRegionPublicId}")
    @Operation(summary = "Get an Organization Region", description = "Get an Organization Region",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public OrganizationRegions getOrganizationRegion(@PathVariable UUID organizationRegionPublicId) {

        return organizationRegionsService.getOrganizationRegion(organizationRegionPublicId);
    }

    @JsonView(BaseView.OrganizationRegionsView.class)
    @GetMapping
    @Operation(summary = "Get all Organization Regions", description = "Get all Organization Regions",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<OrganizationRegions> getAllOrganizationRegions(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                               @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return organizationRegionsService.getAllOrganizationRegions(searchParam, pageable);
    }

    @DeleteMapping("/{organizationRegionPublicId}")
    @Operation(summary = "Delete an Organization Region", description = "Delete an Organization Region",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteOrganizationRegion(@PathVariable UUID organizationRegionPublicId,
                                         @Parameter(hidden = true) Principal principal) {

        organizationRegionsService.deleteOrganizationRegion(organizationRegionPublicId, principal.getName());
    }
}
