package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.OrganizationCountries;
import com.app.itibari.models.request.OrganizationCountriesRequest;
import com.app.itibari.services.OrganizationCountriesService;
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

@Tag(name = "OrganizationCountries", description = "Organization Countries API")
@RestController
@RequestMapping("/organizationCountries")
public class OrganizationCountriesController {

    private final OrganizationCountriesService organizationCountriesService;

    public OrganizationCountriesController(OrganizationCountriesService organizationCountriesService) {

        this.organizationCountriesService = organizationCountriesService;
    }

    @JsonView(BaseView.OrganizationCountriesView.class)
    @PostMapping
    @Operation(summary = "Create a new Organization Country", description = "Create a new Organization Country",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_ORGANIZATION_COUNTRY", value = Examples.CREATE_ORGANIZATION_COUNTRY),
    })))
    public OrganizationCountries createOrganizationCountry(@RequestBody @Valid OrganizationCountriesRequest organizationCountriesRequest,
                                                           @Parameter(hidden = true) Principal principal) {

        return organizationCountriesService.createOrganizationCountry(organizationCountriesRequest, principal.getName());
    }

    @PatchMapping("/{organizationCountryPublicId}")
    @Operation(summary = "Update an Organization Country", description = "Update an Organization Country",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_ORGANIZATION_COUNTRY", value = Examples.CREATE_ORGANIZATION_COUNTRY),
    })))
    public void updateOrganizationCountry(@PathVariable UUID organizationCountryPublicId,
                                          @RequestBody @Valid OrganizationCountriesRequest organizationCountriesRequest,
                                          @Parameter(hidden = true) Principal principal) {

        organizationCountriesService.updateOrganizationCountry(organizationCountryPublicId,
                                                               organizationCountriesRequest,
                                                               principal.getName());
    }

    @JsonView(BaseView.OrganizationCountriesView.class)
    @GetMapping("/{organizationCountryPublicId}")
    @Operation(summary = "Get an Organization Country", description = "Get an Organization Country",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public OrganizationCountries getOrganizationCountry(@PathVariable UUID organizationCountryPublicId) {

        return organizationCountriesService.getOrganizationCountry(organizationCountryPublicId);
    }

    @JsonView(BaseView.OrganizationCountriesView.class)
    @GetMapping
    @Operation(summary = "Get all Organization Countries", description = "Get all Organization Countries",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<OrganizationCountries> getAllOrganizationCountries(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                                   @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return organizationCountriesService.getAllOrganizationCountries(searchParam, pageable);
    }

    @DeleteMapping("/{organizationCountryPublicId}")
    @Operation(summary = "Delete an Organization Country", description = "Delete an Organization Country",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteOrganizationCountry(@PathVariable UUID organizationCountryPublicId,
                                          @Parameter(hidden = true) Principal principal) {

        organizationCountriesService.deleteOrganizationCountry(organizationCountryPublicId, principal.getName());
    }
}
