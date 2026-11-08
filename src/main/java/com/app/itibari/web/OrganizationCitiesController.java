package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.OrganizationCities;
import com.app.itibari.models.request.OrganizationCitiesRequest;
import com.app.itibari.services.OrganizationCitiesService;
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

@Tag(name = "OrganizationCities", description = "Organization Cities API")
@RestController
@RequestMapping("/organizationCities")
public class OrganizationCitiesController {

    private final OrganizationCitiesService organizationCitiesService;

    public OrganizationCitiesController(OrganizationCitiesService organizationCitiesService) {

        this.organizationCitiesService = organizationCitiesService;
    }

    @JsonView(BaseView.OrganizationCitiesView.class)
    @PostMapping
    @Operation(summary = "Create a new Organization City", description = "Create a new Organization City",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_ORGANIZATION_CITY", value = Examples.CREATE_ORGANIZATION_CITY),
    })))
    public OrganizationCities createOrganizationCity(@RequestBody @Valid OrganizationCitiesRequest organizationCitiesRequest,
                                                     @Parameter(hidden = true) Principal principal) {

        return organizationCitiesService.createOrganizationCity(organizationCitiesRequest, principal.getName());
    }

    @PatchMapping("/{organizationCityPublicId}")
    @Operation(summary = "Update an Organization City", description = "Update an Organization City",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_ORGANIZATION_CITY", value = Examples.CREATE_ORGANIZATION_CITY),
    })))
    public void updateOrganizationCity(@PathVariable UUID organizationCityPublicId,
                                       @RequestBody @Valid OrganizationCitiesRequest organizationCitiesRequest,
                                       @Parameter(hidden = true) Principal principal) {

        organizationCitiesService.updateOrganizationCity(organizationCityPublicId, organizationCitiesRequest, principal.getName());
    }

    @JsonView(BaseView.OrganizationCitiesView.class)
    @GetMapping("/{organizationCityPublicId}")
    @Operation(summary = "Get an Organization City", description = "Get an Organization City",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public OrganizationCities getOrganizationCity(@PathVariable UUID organizationCityPublicId) {

        return organizationCitiesService.getOrganizationCity(organizationCityPublicId);
    }

    @JsonView(BaseView.OrganizationCitiesView.class)
    @GetMapping
    @Operation(summary = "Get all Organization Cities", description = "Get all Organization Cities",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<OrganizationCities> getAllOrganizationCities(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                             @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return organizationCitiesService.getAllOrganizationCities(searchParam, pageable);
    }

    @DeleteMapping("/{organizationCityPublicId}")
    @Operation(summary = "Delete an Organization City", description = "Delete an Organization City",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteOrganizationCity(@PathVariable UUID organizationCityPublicId,
                                       @Parameter(hidden = true) Principal principal) {

        organizationCitiesService.deleteOrganizationCity(organizationCityPublicId, principal.getName());
    }
}
