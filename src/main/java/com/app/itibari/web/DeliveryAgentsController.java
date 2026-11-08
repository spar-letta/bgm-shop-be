package com.app.itibari.web;

import com.app.itibari.dataTypes.AcceptTypeEnum;
import com.app.itibari.dataTypes.LocationApprovalStatus;
import com.app.itibari.dataTypes.MobileUserType;
import com.app.itibari.dataTypes.RouteAction;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.LocationApproval;
import com.app.itibari.models.entities.OrderAmendment;
import com.app.itibari.models.request.*;
import com.app.itibari.services.DeliveryAgentsService;
import com.app.itibari.services.DeliveryAgentLoginSessionService;
import com.app.itibari.models.response.DeliveryAgentLoginSessionResponse;
import com.app.itibari.utils.helpers.VersionHelper;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Slf4j
@Tag(name = "DeliveryAgents", description = "Delivery Agents API")
@RestController
@RequestMapping("/deliveryAgents")
public class DeliveryAgentsController {

    private final DeliveryAgentsService deliveryAgentsService;

    private final VersionHelper versionHelper;
    private final DeliveryAgentLoginSessionService loginSessionService;

    public DeliveryAgentsController(DeliveryAgentsService deliveryAgentsService, VersionHelper versionHelper,
                                    DeliveryAgentLoginSessionService loginSessionService) {

        this.deliveryAgentsService = deliveryAgentsService;
        this.versionHelper = versionHelper;
        this.loginSessionService = loginSessionService;
    }

    @PreAuthorize("hasAuthority('CREATE_DELIVERY_AGENT')")
    @JsonView(BaseView.DeliveryAgentsView.class)
    @PostMapping
    @Operation(summary = "Create a new Delivery Agents", description = "Create a new Delivery Agents",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_DELIVERY_AGENT", value = Examples.CREATE_DELIVERY_AGENT),
    })))
    public DeliveryAgents createDeliveryAgent(@RequestBody @Valid DeliveryAgentsRequest deliveryAgentsRequest,
                                              @Parameter(hidden = true) Principal principal) {

        return deliveryAgentsService.createDeliveryAgent(deliveryAgentsRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_DELIVERY_AGENT')")
    @PatchMapping("/{deliveryAgentPublicId}")
    @Operation(summary = "Update a Delivery Agent", description = "Update a Delivery Agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_DELIVERY_AGENT", value = Examples.CREATE_DELIVERY_AGENT),
    })))
    public void updateDeliveryAgent(@PathVariable UUID deliveryAgentPublicId,
                                    @RequestBody @Valid DeliveryAgentsRequest deliveryAgentsRequest,
                                    @Parameter(hidden = true) Principal principal) {

        deliveryAgentsService.updateDeliveryAgent(deliveryAgentPublicId, deliveryAgentsRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_DELIVERY_AGENT')")
    @PatchMapping("/{deliveryAgentPublicId}/availability")
    @Operation(summary = "Update delivery agent availability",
            description = "Set delivery agent availability with a reason from the DeliveryAgentAvailability module")
    public void updateDeliveryAgentAvailability(@PathVariable UUID deliveryAgentPublicId,
                                                @RequestParam boolean available,
                                                @RequestParam UUID reasonPublicId,
                                                @Parameter(hidden = true) Principal principal) {
        deliveryAgentsService.updateDeliveryAgentAvailability(deliveryAgentPublicId, available, reasonPublicId, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @JsonView(BaseView.DeliveryAgentsView.class)
    @GetMapping("/{deliveryAgentPublicId}")
    @Operation(summary = "Get a Delivery Agent", description = "Get a Delivery Agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public DeliveryAgents getDeliveryAgent(@PathVariable UUID deliveryAgentPublicId) {

        return deliveryAgentsService.getDeliveryAgent(deliveryAgentPublicId);
    }

    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @JsonView(BaseView.DeliveryAgentsView.class)
    @GetMapping
    @Operation(summary = "Get all Delivery Agents", description = "Get all Delivery Agents",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<DeliveryAgents> getAllDeliveryAgents(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                     @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return deliveryAgentsService.getAllDeliveryAgents(searchParam, pageable);
    }

    @PreAuthorize("hasAuthority('DELETE_DELIVERY_AGENT')")
    @DeleteMapping("/{deliveryAgentPublicId}")
    @Operation(summary = "Delete a Delivery Agent", description = "Delete a Delivery Agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteDeliveryAgent(@PathVariable UUID deliveryAgentPublicId,
                                    @Parameter(hidden = true) Principal principal) {

        deliveryAgentsService.deleteDeliveryAgent(deliveryAgentPublicId, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_DELIVERY_AGENT')")
    @PatchMapping("/{deliveryAgentPublicId}/generatePassword")
    @Operation(summary = "Generate a new password for a Delivery Agent", description = "Generate a new password for a Delivery Agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void generatePassword(@PathVariable UUID deliveryAgentPublicId,
                                 @Parameter(hidden = true) Principal principal) {

        deliveryAgentsService.generatePassword(deliveryAgentPublicId, principal.getName());
    }

    @PreAuthorize("hasAuthority('UPDATE_DELIVERY_AGENT')")
    @PatchMapping("/{deliveryAgentPublicId}/amendOrder")
    @Operation(summary = "Amend a order", description = "Amend an order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "REQUEST_ORDER_AMENDMENT", value = Examples.CREATE_DELIVERY_AGENT),})))
    public void amendOrder(@PathVariable UUID deliveryAgentPublicId, @RequestBody @Valid AmendOrderRequest amendOrderRequest,
                           @RequestHeader(value = "X-App-Version", required = false) String appVersion,
                           @Parameter(hidden = true) Principal principal) {

        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }
        amendOrderRequest.setDeliveryAgentPublicId(deliveryAgentPublicId);
        deliveryAgentsService.amendOrder(deliveryAgentPublicId, amendOrderRequest, principal.getName());
    }

    // fetch order amendments

    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @JsonView(BaseView.OrderAmendmentView.class)
    @GetMapping("/orderAmendments")
    @Operation(summary = "Get all order amendments", description = "Get all order amendments",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)})
    @PageableAsQueryParam
    public Page<OrderAmendment> getAllOrderAmendments(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                      @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return deliveryAgentsService.getAllOrderAmendments(searchParam, pageable);
    }

    @PreAuthorize("hasAuthority('APPROVE_DELIVERY_AGENT')")
    @PatchMapping("/amendOrder/{orderAmendmentId}")
    @Operation(summary = "Amend a order actions", description = "Amend an order actions",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "REQUEST_ORDER_AMENDMENT", value = Examples.CREATE_DELIVERY_AGENT),
    })))

    public void amendOrderActions(@PathVariable UUID orderAmendmentId,
                                  @RequestBody @Valid AmendRequest amendRequest,
                                  @Parameter(hidden = true) Principal principal) {

        amendRequest.setAmendOrderPublicId(orderAmendmentId);
        deliveryAgentsService.amendOrderActions(amendRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @JsonView(BaseView.DeliveryAgentsView.class)
    @GetMapping("/profile")
    @Operation(summary = "Get the logged in Delivery Agent profile", description = "Get the logged in Delivery Agent profile",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public DeliveryAgents getLoggedInDeliveryAgentProfile(@Parameter(hidden = true) Principal principal,
                                                          @RequestHeader(value = "X-App-Version", required = false) String appVersion) {

        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }
        return deliveryAgentsService.getDeliveryAgentMobileProfile(principal.getName());
    }

    // accept route
    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @PatchMapping("/accept/{routeId}")
    @Operation(summary = "Accept a route", description = "Accept a route",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void acceptRoute(@PathVariable UUID routeId,
                            @RequestParam(name = "acceptType") AcceptTypeEnum acceptType,
                            @RequestHeader(value = "X-App-Version", required = false) String appVersion,
                            @Parameter(hidden = true) Principal principal) {

        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }
        deliveryAgentsService.acceptRoute(routeId, acceptType, principal.getName());
    }

    // start route, end route, cancel route
    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @PatchMapping("/routeActions")
    @Operation(summary = "Start, end or cancel a route", description = "Start, end or cancel a route",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void routeActions(@RequestParam(name = "routeId") UUID routeId,
                             @RequestParam(name = "action") RouteAction action,
                             @RequestHeader(value = "X-App-Version", required = false) String appVersion,
                             @Parameter(hidden = true) Principal principal) {

        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }
        deliveryAgentsService.routeActions(routeId, action, principal.getName());
    }

    // mark order as delivered
    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @PatchMapping("/markOrderAsDelivered/{orderPublicId}")
    @Operation(summary = "Mark an order as delivered", description = "Mark an order as delivered",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void markOrderAsDelivered(@PathVariable UUID orderPublicId,
                                     @RequestParam(name = "routePublicId", required = false) UUID routePublicId,
                                     @RequestBody(required = false) @Valid MarkOrderDeliveredRequest request,
                                     @RequestHeader(value = "X-App-Version", required = false) String appVersion,
                                     @Parameter(hidden = true) Principal principal) throws IOException, InterruptedException {

        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }

        deliveryAgentsService.markOrderAsDelivered(orderPublicId, routePublicId, request, principal.getName());
    }

    // mark order item as delivered

    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @PatchMapping("/markOrderItemAsDelivered")
    @Operation(summary = "Mark an order item as delivered", description = "Mark an order item as delivered",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "MARK ORDER ITEMS AS DELIVERED", value = Examples.MARK_ORDER_ITEMS_AS_DELIVERED),
    })))
    public void markOrderItemAsDelivered(@RequestBody @Valid DeliverItemRequest deliverItemRequest,
                                         @RequestHeader(value = "X-App-Version", required = false) String appVersion,
                                         @Parameter(hidden = true) Principal principal) {

        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }
        deliveryAgentsService.markOrderItemAsDelivered(deliverItemRequest, principal.getName());
    }

    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @PatchMapping("/markVanSaleItemAsSold")
    @Operation(summary = "Mark a van sale item as sold", description = "Mark a van sale item as sold",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "MARK VAN SALE ITEM AS SOLD", value = Examples.MARK_ORDER_ITEMS_AS_DELIVERED),
    })))
    public void markVanSaleItemAsSold(@RequestBody @Valid DeliverVanSaleItemRequest deliverItemRequest,
                                      @RequestHeader(value = "X-App-Version", required = false) String appVersion,
                                      @Parameter(hidden = true) Principal principal) {

        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }
        deliveryAgentsService.markVanSaleItemAsSold(deliverItemRequest, principal.getName());
    }

    // triger customer location verification

    @Deprecated
    @Hidden
    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @PatchMapping("/verifyCustomerLocation/{customerPublicId}")
    @Operation(summary = "Trigger customer location verification", description = "Trigger customer location verification",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void verifyCustomerLocation(@PathVariable UUID customerPublicId,
                                       @RequestHeader(value = "X-App-Version", required = false) String appVersion,
                                       @Parameter(hidden = true) Principal principal) {

        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }
        deliveryAgentsService.verifyCustomerLocation(customerPublicId, principal.getName());
    }

    // verify customer location

    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @PatchMapping("/verifyCustomerLocationWithOtp/{customerPublicId}")
    @Operation(summary = "Verify customer location with otp", description = "Verify customer location with otp",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "VERIFY CUSTOMER LOCATION WITH OTP", value = Examples.VERIFY_CUSTOMER_LOCATION_WITH_OTP),
    })))
    public void verifyCustomerLocationWithOtp(@PathVariable UUID customerPublicId,
                                              @RequestBody @Valid VerifyCustomerLocationRequest verifyCustomerLocationRequest,
                                              @RequestHeader(value = "X-App-Version", required = false) String appVersion,
                                              @Parameter(hidden = true) Principal principal) {

        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }

        deliveryAgentsService.verifyCustomerLocationWithOtp(customerPublicId, verifyCustomerLocationRequest, principal.getName());
    }

    //    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @PatchMapping("/{salesOrderPublicId}/cancelOrder")
    @Operation(summary = "Cancel order", description = "Cancel order",
            responses = {@ApiResponse(responseCode = "200", description = "OK"),
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CANCEL ORDER", value = Examples.CANCEL_ORDER),})))
    public void cancelOrder(@PathVariable(name = "salesOrderPublicId") UUID salesOrderPublicId,
                            @Valid @RequestBody CancelReasonRequest cancelReasonRequest,
                            @RequestHeader(value = "X-App-Version", required = false) String appVersion,
                            @Parameter(hidden = true) Principal principal) {

        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }
        deliveryAgentsService.cancelOrder(salesOrderPublicId, cancelReasonRequest, principal.getName());
    }


    // send otp to customer

    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @PatchMapping("/sendOtpToCustomer/{orderPublicId}")
    @Operation(summary = "Send OTP to customer", description = "Send OTP to customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public String sendOtpToCustomer(@PathVariable UUID orderPublicId,
                                    @RequestHeader(value = "X-App-Version", required = false) String appVersion,
                                    @Parameter(hidden = true) Principal principal) {
        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }
        return deliveryAgentsService.sendOtpToCustomer(orderPublicId, principal.getName());
    }

    // request location approval

    @PatchMapping("/requestLocationApproval/{orderPublicId}")
    @Operation(summary = "Request location approval from customer", description = "Request location approval from customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void requestLocationApproval(@PathVariable UUID orderPublicId,
                                        @RequestBody LocationApprovalRequest locationApprovalRequest,
                                        @RequestHeader(value = "X-App-Version", required = false) String appVersion,
                                        @Parameter(hidden = true) Principal principal) {
        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }
        locationApprovalRequest.setOrderPublicId(orderPublicId);
        deliveryAgentsService.requestLocationApproval(locationApprovalRequest, principal.getName());
    }

    // fetch location approvals

    @JsonView(BaseView.LocationApprovalView.class)
    @GetMapping("/locationApprovals")
    @Operation(summary = "Fetch location approval requests", description = "Fetch location approval requests",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<LocationApproval> getLocationApprovals(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                       @RequestParam(name = "statuses" , required = false) List<LocationApprovalStatus>  statuses,
                                                       @RequestParam(name = "deliveryAgent" , required = false) String deliveryAgent,
                                                       @RequestParam(name = "customer" , required = false) String customer,
                                                       @RequestParam(name = "from" , required = false) String from,
                                                       @RequestParam(name = "to" , required = false) String to,
                                                       @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
                                                       @Parameter(hidden = true) Principal principal) {
        return deliveryAgentsService.getLocationApprovals(searchParam, statuses, deliveryAgent, customer, from, to, pageable);
    }

    // approve or reject location approval

    @PatchMapping("/locationApproval/{locationApprovalId}")
    @Operation(summary = "Approve or reject location approval request", description = "Approve or reject location approval request",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void approveOrRejectLocationApproval(@PathVariable UUID locationApprovalId,
                                            @RequestParam(name = "approve") boolean approve,
                                            @RequestParam(name = "rejectionCodePublicId",required = false) UUID rejectionCodePublicId,
                                            @Parameter(hidden = true) Principal principal) {
        deliveryAgentsService.approveOrRejectLocationApproval(locationApprovalId, approve,rejectionCodePublicId, principal.getName());
    }


    // unlock order and route

    @PatchMapping("/unlock/{locationApprovalPublicId}")
    @Operation(summary = "Unlock order and route", description = "Unlock order and route",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void unlockOrderAndRoute(@PathVariable UUID locationApprovalPublicId,
                                   @Parameter(hidden = true) Principal principal) {
        // unlock order and route

        deliveryAgentsService.unlockLocationApprovalRequest(locationApprovalPublicId, principal.getName());

    }

    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @JsonView(BaseView.DeliveryAgentsMinifiedView.class)
    @GetMapping("/minifiedData")
    @Operation(summary = "Get all Delivery Agent Mini Data", description = "Get all Delivery Agent Mini Data",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<DeliveryAgents> getDeliveryAgentMiniData(@RequestParam(name = "searchParam", required = false) String searchParam) {

        return deliveryAgentsService.getDeliveryAgentMiniData(searchParam);
    }

    @PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @PatchMapping("/sellVansale")
    @Operation(summary = "Sell Van sales", description = "Sell Van sales",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "SELL VAN SALE ", value = Examples.SELL_VAN_SALE_ITEM),
    })))
    public void sellVanSaleItem(@RequestBody @Valid SellVanSaleRequest deliverItemRequest,
                                @RequestHeader(value = "X-App-Version", required = false) String appVersion,
                                @Parameter(hidden = true) Principal principal) {

        if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
            versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.DeliveryAgent);
        }
        deliveryAgentsService.sellVanSaleItem(deliverItemRequest, principal.getName());
    }

    @PatchMapping("/amendOrderSendExcel")
    public void amendOrderSendExcel(@Parameter(hidden = true) Principal principal) throws IOException {

        deliveryAgentsService.sendAmendmentToDepartmentOfficers();
    }

    //@PreAuthorize("hasAuthority('VIEW_DELIVERY_AGENT')")
    @GetMapping("/loginSessions")
    @Operation(summary = "Delivery-agent login session report")
    public Page<DeliveryAgentLoginSessionResponse> getLoginSessions(
            @RequestParam(required = false) UUID deliveryAgentPublicId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "loginTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return loginSessionService.report(deliveryAgentPublicId, active, from, to, pageable);
    }

    /**
     * this only is used by DA ,
     * @param authentication
     */

    //@PreAuthorize("hasRole('DELIVERY_AGENT')")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Log out the current delivery-agent session")
    public void logout(JwtAuthenticationToken authentication) {
        UUID sessionId = UUID.fromString(authentication.getToken().getClaimAsString("session_id"));
        loginSessionService.logout(sessionId, authentication.getToken().getSubject());
    }
}
