package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.OrganizationLocations;
import com.app.itibari.models.request.OrganizationLocationsRequest;
import com.app.itibari.services.OrganizationLocationsService;
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

@Tag(name = "OrganizationLocations", description = "Organization Locations API")
@RestController
@RequestMapping("/organizationLocations")
public class OrganizationLocationsController {

    private final OrganizationLocationsService organizationLocationsService;

    public OrganizationLocationsController(OrganizationLocationsService organizationLocationsService) {

        this.organizationLocationsService = organizationLocationsService;
    }

    @JsonView(BaseView.OrganizationLocationsView.class)
    @PostMapping
    @Operation(summary = "Create a new Organization Location", description = "Create a new Organization Location",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_ORGANIZATION_LOCATION", value = Examples.CREATE_ORGANIZATION_LOCATION),
    })))
    public OrganizationLocations createOrganizationLocation(@RequestBody @Valid OrganizationLocationsRequest organizationLocationsRequest,
                                                            @Parameter(hidden = true) Principal principal) {

        return organizationLocationsService.createOrganizationLocation(organizationLocationsRequest, principal.getName());
    }

    @PatchMapping("/{organizationLocationPublicId}")
    @Operation(summary = "Update an Organization Location", description = "Update an Organization Location",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_ORGANIZATION_LOCATION", value = Examples.CREATE_ORGANIZATION_LOCATION),
    })))
    public void updateOrganizationLocation(@PathVariable UUID organizationLocationPublicId,
                                           @RequestBody @Valid OrganizationLocationsRequest organizationLocationsRequest,
                                           @Parameter(hidden = true) Principal principal) {

        organizationLocationsService.updateOrganizationLocation(organizationLocationPublicId,
                                                                organizationLocationsRequest,
                                                                principal.getName());
    }

    @JsonView(BaseView.OrganizationLocationsView.class)
    @GetMapping("/{organizationLocationPublicId}")
    @Operation(summary = "Get an Organization Location", description = "Get an Organization Location",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public OrganizationLocations getOrganizationLocation(@PathVariable UUID organizationLocationPublicId) {

        return organizationLocationsService.getOrganizationLocation(organizationLocationPublicId);
    }

    @JsonView(BaseView.OrganizationLocationsView.class)
    @GetMapping
    @Operation(summary = "Get all Organization Locations", description = "Get all Organization Locations",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<OrganizationLocations> getAllOrganizationLocations(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                                   @RequestParam(name = "name", required = false) String name,
                                                                   @RequestParam(name = "organizationRegionId", required = false) UUID organizationRegionId,
                                                                   @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return organizationLocationsService.getAllOrganizationLocations(searchParam, name, organizationRegionId, pageable);
    }

    @DeleteMapping("/{organizationLocationPublicId}")
    @Operation(summary = "Delete an Organization Location", description = "Delete an Organization Location",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteOrganizationLocation(@PathVariable UUID organizationLocationPublicId,
                                           @Parameter(hidden = true) Principal principal) {

        organizationLocationsService.deleteOrganizationLocation(organizationLocationPublicId, principal.getName());
    }
}
