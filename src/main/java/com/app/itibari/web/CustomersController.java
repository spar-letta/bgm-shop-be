package com.app.itibari.web;

import com.app.itibari.dataTypes.ChurnedCustomerLeadStatus;
import com.app.itibari.dataTypes.ChurnedDateRangeFilter;
import com.app.itibari.dataTypes.CustomerOutboundCallStatus;
import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.dataTypes.SourceChannelEnum;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.CallStatus;
import com.app.itibari.models.entities.ChurnedCallHistory;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.request.CustomerAction;
import com.app.itibari.models.request.CustomersRequest;
import com.app.itibari.models.request.UpdateCustomerCallStatusRequest;
import com.app.itibari.models.request.UpdateCustomerLeadStatusRequest;
import com.app.itibari.models.response.ChurnedCustomer;
import com.app.itibari.models.response.CustomerLocationResponse;
import com.app.itibari.models.response.CustomerPageable;
import com.app.itibari.models.response.OrderStats;
import com.app.itibari.services.CustomersService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
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
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Customers", description = "Customers API")
@RestController
@RequestMapping("/customers")
public class CustomersController {

    private final CustomersService customersService;

    public CustomersController(CustomersService customersService) {

        this.customersService = customersService;
    }

    @JsonView(BaseView.CustomersView.class)
    @PostMapping
    @Operation(summary = "Create a new Customer", description = "Create a new Customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_CUSTOMER", value = Examples.CREATE_CUSTOMER),
            @ExampleObject(name = "CREATE_CUSTOMER_WITH_PARENT", value = Examples.CREATE_CUSTOMER_WITH_PARENT),
    })))
    public Customers createCustomer(@RequestBody @Valid CustomersRequest customersRequest,
                                    @Parameter(hidden = true) Principal principal) {

        return customersService.createCustomer(customersRequest, principal.getName());
    }

    @PatchMapping("/{customerPublicId}")
    @Operation(summary = "Update a Customer", description = "Update a Customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_CUSTOMER", value = Examples.CREATE_CUSTOMER),
    })))
    public void updateCustomer(@PathVariable UUID customerPublicId, @RequestBody @Valid CustomersRequest customersRequest,
                               @Parameter(hidden = true) Principal principal) {
        customersService.updateCustomer(customerPublicId, customersRequest, principal.getName());
    }

    @JsonView(BaseView.CustomersView.class)
    @GetMapping("/{customerPublicId}")
    @Operation(summary = "Get a Customer", description = "Get a Customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Customers getCustomer(@PathVariable UUID customerPublicId) {

        return customersService.getCustomer(customerPublicId);
    }

    @JsonView(BaseView.CustomersView.class)
    @GetMapping
    @Operation(
            summary = "Get all Customers",
            description = "Get all Customers",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<Customers> getAllCustomers(@RequestParam(name = "searchParam", required = false) String searchParam,
                                           @RequestParam(name = "name", required = false) String name,
                                           @RequestParam(name = "email", required = false) String email,
                                           @RequestParam(name = "nationalId", required = false) String nationalId,
                                           @RequestParam(name = "phoneNumber", required = false) String phoneNumber,
                                           @RequestParam(name = "sourceChannel", required = false) SourceChannelEnum sourceChannel,
                                           @RequestParam(name = "organizationLocationId", required = false) UUID organizationLocationId,
                                           @RequestParam(name = "locations", required = false) List<UUID> locations,
                                           @RequestParam(name = "organizationRegionId", required = false) UUID organizationRegionId,
                                           @RequestParam(name = "registeredBy", required = false) UUID registeredBy,
                                           @RequestParam(name = "servedBy", required = false) UUID servedBy,
                                           @RequestParam(name = "isFromSA", required = false) Boolean isFromSA,
                                           @RequestParam(name = "active", required = false) Boolean active,
                                           @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
                                           @RequestParam(name = "salesAgentPublicId", required = false) UUID salesAgentPublicId,
                                           @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                           @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                           @Parameter(hidden = true) Principal principal,
                                           @PageableDefault(size = 20, sort = "registrationDate", direction = Sort.Direction.DESC) Pageable pageable) {

        return customersService.getAllCustomers(searchParam,
                name,
                email,
                nationalId,
                phoneNumber,
                sourceChannel,
                organizationLocationId,
                organizationRegionId,
                registeredBy,
                locations,
                startDate,
                endDate,
                servedBy,
                parentCustomerPublicId,
                salesAgentPublicId,
                isFromSA,
                principal,
                active,
                pageable);
    }

    @DeleteMapping("/{customerPublicId}")
    @Operation(summary = "Delete a Customer", description = "Delete a Customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteCustomer(@PathVariable UUID customerPublicId,
                               @Parameter(hidden = true) Principal principal) {

        customersService.deleteCustomer(customerPublicId, principal.getName());
    }

    @JsonView(BaseView.CustomersView.class)
    @GetMapping("/orderStats")
    @Operation(
            summary = "Get Customer Order Stats",
            description = "Get the number of orders and average order value for a customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public OrderStats getCustomerOrderStats(@RequestParam(name = "customerId") UUID customerId) {

        return customersService.getCustomerOrderStats(customerId);
    }

    @JsonView(BaseView.CustomersView.class)
    @GetMapping("/profile")
    @Operation(
            summary = "Get Customer Profile",
            description = "Get the profile of a customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Customers getCustomerProfile(@Parameter(hidden = true) Principal principal) {

        return customersService.getCustomerProfile(principal.getName());
    }

    @GetMapping("/customerLocations")
    @Operation(
            summary = "Get Customer Locations",
            description = "Get all customer locations for a given customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public List<CustomerLocationResponse> getCustomerLocations() {

        return customersService.getCustomerLocations();
    }

    // supplier customer fetch

    @GetMapping("/supplierCustomers")
    @Operation(
            summary = "Get Supplier Customers",
            description = "Get all customers associated with a supplier",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public CustomerPageable getSupplierCustomers(
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 100, direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {

        return customersService.getSupplierCustomers(regionId, startDate, endDate, pageable,
                principal);
    }

    @PatchMapping("/{customerPublicId}/showSupplierCustomerPhoneNumber")
    @Operation(summary = "Show Supplier Customer Phone Number", description = "Show Supplier Customer Phone Number",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void showSupplierCustomerPhoneNumber(@PathVariable UUID customerPublicId,
                                                @Parameter(hidden = true) Principal principal) {

        customersService.showSupplierCustomerPhoneNumber(customerPublicId, principal.getName());
    }

    @Hidden
    @GetMapping("/fixDuplicate")
    @Operation(summary = "Get a Customer", description = "Get a Customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK")})
    public void getCustomerFixDuplicate() {

        customersService.fixCustomerDuplicate();
    }

    // change customer type

    @PatchMapping("/{customerPublicId}/changeCustomerType")
    @Operation(summary = "Change Customer Type", description = "Change Customer Type",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void changeCustomerType(@PathVariable UUID customerPublicId,
                                   @RequestParam(name = "customerType") CustomerType customerType,
                                   @Parameter(hidden = true) Principal principal) {
        customersService.changeCustomerType(customerPublicId, customerType, principal.getName());
    }


    //    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{customerPublicId}/actions")
    @Operation(summary = "Update a Customer", description = "Update a Customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "DE_ACTIVATE_CUSTOMER", value = Examples.DE_ACTIVATE_CUSTOMER),
            @ExampleObject(name = "APPROVE_DE_ACTIVATE_CUSTOMER", value = Examples.APPROVE_DE_ACTIVATE_CUSTOMER),
            @ExampleObject(name = "ACTIVATE_CUSTOMER", value = Examples.ACTIVATE_CUSTOMER),})))
    public void CustomerActions(@PathVariable UUID customerPublicId,
                                @RequestBody CustomerAction customerAction,
                                @Parameter(hidden = true) Principal principal) {
        customersService.customerActions(customerPublicId, customerAction, principal.getName());
    }

    @JsonView(BaseView.SupplierCustomerView.class)
    @GetMapping("/supplierCustomerOnSalesOrder")
    @Operation(
            summary = "Get all Customers for supplier",
            description = "Get all Customers for supplier",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    @PageableAsQueryParam
    public Page<Customers> getAllCustomers(
            @RequestParam(name = "regionId", required = false) UUID regionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 20) Pageable pageable) {

        return customersService.getAllSupplierCustomers(regionId, locationId, searchParam,
                startDate,
                endDate,
                principal,
                pageable);
    }

    @JsonView(BaseView.CustomerFewDetailView.class)
    @GetMapping("/naPoneNumber")
    @Operation(summary = "Get a Customer By Phone", description = "Get a Customer By Phone",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Customers getCustomerByPhone(@RequestParam(name = "phoneNumber") String customerPhoneNumber) {

        return customersService.getCustomerByPhone(customerPhoneNumber);
    }

    @GetMapping("/churnedCustomers")
    @Operation(summary = "Get a Churned Customers", description = "Get a Churned Customers",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ChurnedCustomer> getChurnedCustomer(
            @RequestParam(name = "rangeFilter", required = false) ChurnedDateRangeFilter rangeFilter,
            @RequestParam(name = "customerType", required = false) CustomerType customerType,
            @RequestParam(name = "callStatusPublicId", required = false) UUID callStatusPublicId,
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "departmentPublicId", required = false) UUID departmentPublicId,
            @RequestParam(name = "callStatus", required = false) CustomerOutboundCallStatus callStatus,
            @RequestParam(name = "leadStatus", required = false) ChurnedCustomerLeadStatus leadStatus,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        return customersService.getChurnedCustomer(rangeFilter, customerType, callStatusPublicId, startDate, endDate, searchParam, callStatus, departmentPublicId, leadStatus, pageable);
    }

    // @PreAuthorize("hasAuthority('UPDATE_CUSTOMER_CALL_STATUS')")
    @PatchMapping("/{publicId}/callStatus")
    @Operation(summary = "Set call status on a customer",
            description = "Records the outcome of an outreach call against a churned customer. " +
                    "{publicId} is the customer's public UUID. Body status must be a CustomerOutboundCallStatus " +
                    "value; each maps to an active call_status catalog row with the same code.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request — unknown outcome or inactive catalog row for that code", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_CUSTOMER_CALL_STATUS", value = Examples.UPDATE_CUSTOMER_CALL_STATUS)
    })))
    public void updateCustomerCallStatus(@PathVariable(name = "publicId") UUID publicId,
                                         @RequestBody @Valid UpdateCustomerCallStatusRequest request,
                                         @Parameter(hidden = true) Principal principal) {
        customersService.updateCustomerCallStatus(publicId, request, principal.getName());
    }

    @PatchMapping("/{publicId}/leadStatus")
    @Operation(summary = "Set lead status on a customer",
            description = "Sets GREEN / YELLOW / RED classification on a churned customer. {publicId} is the customer's public UUID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_CUSTOMER_LEAD_STATUS", value = Examples.UPDATE_CUSTOMER_LEAD_STATUS)
    })))
    public void updateCustomerLeadStatus(@PathVariable(name = "publicId") UUID publicId,
                                         @RequestBody @Valid UpdateCustomerLeadStatusRequest request,
                                         @Parameter(hidden = true) Principal principal) {
        customersService.updateCustomerLeadStatus(publicId, request, principal.getName());
    }

    @GetMapping("/nonChurnedInActiveCustomers")
    @Operation(summary = "Get non-churned in-active customers", description = "Get non-churned in-active customers",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<ChurnedCustomer> getNonChurnedInActiveCustomers(
            @RequestParam(name = "customerType", required = false) CustomerType customerType,
            @RequestParam(name = "callStatusPublicId", required = false) UUID callStatusPublicId,
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "inActive", required = false) Boolean inActive,
            @RequestParam(name = "departmentPublicId", required = false) UUID departmentPublicId,
            @RequestParam(name = "callStatus", required = false) CustomerOutboundCallStatus callStatus,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        return customersService.getNonChurnedInActiveCustomers(customerType, callStatusPublicId, startDate, endDate, searchParam, inActive, callStatus, departmentPublicId, pageable);
    }

    // activate/ deactivate child customer from mobile app

    @PatchMapping("/{customerPublicId}/activateDeactivateChildCustomer")
    @Operation(summary = "Activate/Deactivate Child Customer", description = "Activate/Deactivate Child Customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void activateDeactivateChildCustomer(@PathVariable UUID customerPublicId,
                                                @RequestParam(name = "activate") Boolean activate,
                                                @Parameter(hidden = true) Principal principal) {
        customersService.activateDeactivateChildCustomer(customerPublicId, activate, principal.getName());
    }

    @GetMapping("/allCallHistory")
    public Page<ChurnedCallHistory> getChurnedCallHistory(
            @PageableDefault(size = 20) Pageable pageable) {

        return customersService.getChurnedCallHistory(pageable);
    }

    @GetMapping("/{publicId}/callHistory")
    @Operation(summary = "Get a customer's call history",
            description = "Last contacts for {publicId}, newest first. Empty page is 200 when the customer exists.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {
                            @ExampleObject(name = "FETCH_CUSTOMER_CALL_HISTORY", value = Examples.FETCH_CUSTOMER_CALL_HISTORY)
                    })),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<ChurnedCallHistory> getCustomerCallHistory(
            @PathVariable(name = "publicId") UUID publicId,
            @PageableDefault(size = 10) Pageable pageable) {
        return customersService.getCustomerCallHistory(publicId, pageable);
    }
}
