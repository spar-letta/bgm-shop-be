package com.app.itibari.web;

import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.DiscountApproval;
import com.app.itibari.models.entities.SalesAgents;
import com.app.itibari.models.entities.SalesSummaryDto;
import com.app.itibari.models.request.AgentAction;
import com.app.itibari.models.request.SalesAgentsRequest;
import com.app.itibari.models.response.SalesAgentPerformance;
import com.app.itibari.services.SalesAgentsService;
import com.app.itibari.services.SalesOrdersService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "SalesAgents", description = "Sales Agents API")
@RestController
@RequestMapping("/salesAgents")
public class SalesAgentsController {

    private final SalesAgentsService salesAgentsService;
    private final SalesOrdersService salesOrdersService;

    public SalesAgentsController(SalesAgentsService salesAgentsService, SalesOrdersService salesOrdersService) {

        this.salesAgentsService = salesAgentsService;
        this.salesOrdersService = salesOrdersService;
    }

    @JsonView(BaseView.SalesAgentsView.class)
    @PostMapping
    @Operation(summary = "Create a new Sales Agent", description = "Create a new Sales Agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_SALES_AGENT", value = Examples.CREATE_SALES_AGENT),
    })))
    public SalesAgents createSalesAgent(@RequestBody @Valid SalesAgentsRequest salesAgentsRequest,
                                        @Parameter(hidden = true) Principal principal) {

        return salesAgentsService.createSalesAgent(salesAgentsRequest, principal.getName());
    }

    @PatchMapping("/{salesAgentPublicId}")
    @Operation(summary = "Update a Sales Agent", description = "Update a Sales Agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_SALES_AGENT", value = Examples.CREATE_SALES_AGENT),
    })))
    public void updateSalesAgent(@PathVariable UUID salesAgentPublicId, @RequestBody @Valid SalesAgentsRequest salesAgentsRequest,
                                 @Parameter(hidden = true) Principal principal) {

        salesAgentsService.updateSalesAgent(salesAgentPublicId, salesAgentsRequest, principal.getName());
    }

    @JsonView(BaseView.SalesAgentsView.class)
    @GetMapping("/{salesAgentPublicId}")
    @Operation(summary = "Get a Sales Agent", description = "Get a Sales Agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public SalesAgents getSalesAgent(@PathVariable UUID salesAgentPublicId) {

        return salesAgentsService.getSalesAgent(salesAgentPublicId);
    }

    @JsonView(BaseView.SalesAgentsView.class)
    @GetMapping
    @Operation(summary = "Get all Sales Agents", description = "Get all Sales Agents",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<SalesAgents> getAllSalesAgents(@RequestParam(name = "searchParam", required = false) String searchParam,
                                               @RequestParam(name = "organisationRegionPublicId", required = false) UUID organisationRegionPublicId,
                                               @RequestParam(name = "organisationLocationPublicId", required = false) UUID organisationLocationPublicId,
                                               @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return salesAgentsService.getAllSalesAgents(searchParam, organisationRegionPublicId, organisationLocationPublicId, pageable);
    }

    @DeleteMapping("/{salesAgentPublicId}")
    @Operation(summary = "Delete a Sales Agent", description = "Delete a Sales Agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteSalesAgent(@PathVariable UUID salesAgentPublicId,
                                 @Parameter(hidden = true) Principal principal) {

        salesAgentsService.deleteSalesAgent(salesAgentPublicId, principal.getName());
    }

    @GetMapping("/profile")
    @JsonView(BaseView.SalesAgentsView.class)
    @Operation(summary = "Get Sales Agent Profile", description = "Get Sales Agent Profile",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public SalesAgents getSalesAgentProfile(@Parameter(hidden = true) Principal principal) {

        return salesAgentsService.getSalesAgentProfile(principal.getName());
    }

    @GetMapping("/{salesAgentPublicId}/performance")
    @Operation(summary = "Get Sales Agent Performance", description = "Get Sales Agent Performance",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ResponseEntity<SalesAgentPerformance> getSalesAgentPerformance(@PathVariable UUID salesAgentPublicId,
                                                                          @RequestParam(name = "from") String from,
                                                                          @RequestParam(name = "to") String to,
                                                                          @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        SalesAgentPerformance salesAgentPerformance = salesAgentsService.getSalesAgentPerformance(salesAgentPublicId, from, to, pageable);
        return ResponseEntity.ok(salesAgentPerformance);
    }

    // deactivate sales agent
    @PatchMapping("/{salesAgentPublicId}/deactivate")
    @Operation(summary = "Deactivate a Sales Agent", description = "Deactivate a Sales Agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deactivateSalesAgent(@PathVariable UUID salesAgentPublicId,
                                     @Parameter(hidden = true) Principal principal) {

        salesAgentsService.deactivateSalesAgent(salesAgentPublicId, principal.getName());
    }

    @PatchMapping("/{salesAgentPublicId}/actions")
    @Operation(summary = "Actions on a Sales Agent", description = "Actions on a Sales Agent",
            responses = {@ApiResponse(responseCode = "200", description = "OK")}, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "MAP CUSTOMER TO SA", value = Examples.MAP_CUSTOMER_TO_SA),
            @ExampleObject(name = "DE ACTIVATE SA", value = Examples.DE_ACTIVATE_SA),
    })))
    public void actionSalesAgent(@PathVariable UUID salesAgentPublicId,
                                 @Valid @RequestBody AgentAction agentAction,
                                 @Parameter(hidden = true) Principal principal) {

        salesAgentsService.actionSalesAgent(salesAgentPublicId, agentAction, principal.getName());
    }

    // activate sales agent
    @PatchMapping("/{salesAgentPublicId}/activate")
    @Operation(summary = "Activate a Sales Agent", description = "Activate a Sales Agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void activateSalesAgent(@PathVariable UUID salesAgentPublicId,
                                   @Parameter(hidden = true) Principal principal) {

        salesAgentsService.activateSalesAgent(salesAgentPublicId, principal.getName());
    }

    // fetch special requests

    @JsonView(BaseView.DiscountApprovalView.class)
    @GetMapping("/specialRequests")
    @Operation(summary = "Fetch Special Requests", description = "Fetch Special Requests",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<DiscountApproval> fetchSpecialRequests(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return salesAgentsService.fetchSpecialRequests(startDate, endDate, principal.getName(), pageable);
    }


//    @JsonView(BaseView.DiscountApprovalView.class)
    @GetMapping("/salesAgentOderSummary")
    @Operation(summary = "Fetch Special Requests", description = "Fetch Special Requests",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    @PageableAsQueryParam
    public Page<SalesSummaryDto> fetchSalesAgentOderSummary(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "organisationRegionPublicId", required = false) UUID organisationRegionPublicId,
            @RequestParam(name = "organisationLocationPublicId", required = false) UUID organisationLocationPublicId,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return salesOrdersService.fetchSalesAgentOderSummary(organisationRegionPublicId, organisationLocationPublicId, startDate, endDate, searchParam, principal, pageable);
    }

    @JsonView(BaseView.SalesAgentsMinifiedView.class)
    @GetMapping("/minifiedData")
    @Operation(summary = "Get all Sales Agents minified data", description = "Get all Sales Agents minified data",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<SalesAgents> getAllSalesAgentsMiniData(@RequestParam(name = "searchParam", required = false) String searchParam) {

        return salesAgentsService.getAllSalesAgentsMiniData(searchParam);
    }

}
