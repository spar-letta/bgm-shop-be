package com.app.itibari.web;

import com.app.itibari.dataTypes.*;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.CustomerReturnItem;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.*;
import com.app.itibari.services.DeliveryAgentsService;
import com.app.itibari.services.SalesOrdersService;
import com.app.itibari.utils.helpers.AgentActiveChecker;
import com.app.itibari.utils.helpers.VersionHelper;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(name = "Sales Orders", description = "Sales Orders API")
@RestController
@RequestMapping("/salesOrders")
public class SalesOrdersController {

    private final SalesOrdersService salesOrdersService;

    private final VersionHelper versionHelper;

    private final AgentActiveChecker agentActiveChecker;
    private final DeliveryAgentsService deliveryAgentsService;

    public SalesOrdersController(SalesOrdersService salesOrdersService,
                                 VersionHelper versionHelper,
                                 AgentActiveChecker agentActiveChecker, DeliveryAgentsService deliveryAgentsService) {

        this.salesOrdersService = salesOrdersService;
        this.versionHelper = versionHelper;
        this.agentActiveChecker = agentActiveChecker;
        this.deliveryAgentsService = deliveryAgentsService;
    }

    @JsonView(BaseView.SalesOrdersView.class)
    @PostMapping
    @Operation(summary = "Create a new Sales Order", description = "Create a new Sales Order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_SALES_ORDER", value = Examples.CREATE_SALES_ORDERS),
    })))
    public SalesOrders createSalesOrder(@RequestBody @Valid SalesOrdersRequest salesOrdersRequest,
                                        @Parameter(hidden = true) Principal principal) {

        return salesOrdersService.createSalesOrder(salesOrdersRequest, principal.getName());
    }

    @PatchMapping("/{salesOrderPublicId}")
    @Operation(summary = "Update a Sales Order", description = "Update a Sales Order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_SALES_ORDER", value = Examples.CREATE_SALES_ORDERS),
    })))
    public void updateSalesOrder(@PathVariable UUID salesOrderPublicId,
                                 @RequestBody @Valid SalesOrdersRequest SalesOrdersRequest,
                                 @RequestParam(name = "isCustomerInitiated", required = false, defaultValue = "false") Boolean isCustomerInitiated,
                                 @RequestParam(name = "isSalesAgentInitiated", required = false, defaultValue = "false") Boolean isSalesAgentInitiated,
                                 @Parameter(hidden = true) Principal principal) {

        salesOrdersService.updateSalesOrder(salesOrderPublicId, SalesOrdersRequest, principal.getName(), isCustomerInitiated, isSalesAgentInitiated);
    }

    @JsonView(BaseView.SalesOrdersView.class)
    @GetMapping("/{salesOrderPublicId}")
    @Operation(summary = "Get a Sales Order", description = "Get a Sales Order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public SalesOrders getSalesOrder(@PathVariable UUID salesOrderPublicId) {

        return salesOrdersService.getSalesOrder(salesOrderPublicId);
    }

    @JsonView(BaseView.SalesOrdersView.class)
    @GetMapping
    @Operation(
            summary = "Get all Sales Orders",
            description = "Get all Sales Orders",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public SalesOrdersPageableDTO getAllSalesOrders(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "salesOrderNumber", required = false) String salesOrderNumber,
            @RequestParam(name = "customerId", required = false) UUID customerId,
            @RequestParam(name = "warehouseId", required = false) UUID warehouseId,
            @RequestParam(name = "organizationRegionId", required = false) UUID organizationRegionId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "sourceChannel", required = false) SourceChannelEnum sourceChannel,
            @RequestParam(name = "salesOrderStatus", required = false) LoadingStatusEnum salesOrderStatus,
            @RequestParam(name = "statuses", required = false) List<LoadingStatusEnum> statuses,
            @RequestParam(name = "paymentStatus", required = false) PaymentStatusEnum paymentStatus,
            @RequestParam(name = "invoiceNumber", required = false) String invoiceNumber,
            @RequestParam(name = "bookedBySalesAgent", required = false) Boolean bookedBySalesAgent,
            @RequestParam(name = "salesAgentPublicId", required = false) UUID salesAgentPublicId,
            @RequestParam(name = "deliveryAgentPublicId", required = false) UUID deliveryAgentPublicId,
            @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
            @RequestParam(name = "bookedByParent", required = false) Boolean bookedByParent,
            @RequestParam(name = "isCredit", required = false) Boolean isCredit,
            @RequestParam(name = "isWalkIn", required = false) Boolean isWalkIn,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return salesOrdersService.getAllSalesOrders(searchParam,
                salesOrderNumber,
                customerId,
                warehouseId,
                organizationRegionId,
                sourceChannel,
                salesOrderStatus,
                paymentStatus,
                invoiceNumber,
                startDate,
                endDate,
                locationId,
                bookedBySalesAgent,
                salesAgentPublicId,
                deliveryAgentPublicId,
                parentCustomerPublicId,
                isCredit,
                statuses,
                isWalkIn,
                bookedByParent,
                pageable
        );
    }

    @DeleteMapping("/{salesOrderPublicId}")
    @Operation(summary = "Delete a Sales Order", description = "Delete a Sales Order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteSalesOrder(@PathVariable UUID salesOrderPublicId,
                                 @Parameter(hidden = true) Principal principal) {

        salesOrdersService.deleteSalesOrder(salesOrderPublicId, principal.getName());
    }

    // get sales order items

    @JsonView(BaseView.SalesOrdersView.class)
    @GetMapping("/{salesOrderPublicId}/items")
    @Operation(summary = "Get Sales Order Items", description = "Get Sales Order Items",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<SalesOrderItems> getSalesOrderItems(@PathVariable UUID salesOrderPublicId,
                                                    @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return salesOrdersService.getSalesOrderItems(salesOrderPublicId, pageable);
    }

    @GetMapping("/{salesOrderPublicId}/items/slim")
    @Operation(summary = "Get Sales Order Items (slim)", description = "Returns a reduced payload with only essential item and product fields",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<SalesOrderItemSlimDTO> getSalesOrderItemsSlim(@PathVariable UUID salesOrderPublicId,
                                                              @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return salesOrdersService.getSalesOrderItemsSlim(salesOrderPublicId, pageable);
    }

    // get all sales orderItems per agent.

    @JsonView(BaseView.SalesOrdersView.class)
    @GetMapping("/deliveryAgent/{deliveryAgentPublicId}/items")
    @Operation(summary = "Get all Sales Order Items per agent", description = "Get all Sales Order Items per agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<SalesOrderItems> getSalesOrderItemsPerAgent(@PathVariable UUID deliveryAgentPublicId,
                                                            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return salesOrdersService.getSalesOrderItemsPerAgent(deliveryAgentPublicId, pageable);
    }

      /*
    mobile customer orders
     */

    @JsonView(BaseView.SalesOrdersView.class)
    @PostMapping("/customer/placeOrder")
    @Operation(summary = "Place Order", description = "Place Order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_SALES_ORDER", value = Examples.CREATE_customer_ORDERS),
    })))
    public SalesOrders placeOrder(
            @RequestBody @Valid SalesOrdersRequest salesOrdersRequest,
            @RequestHeader(value = "X-App-Version", required = false) String appVersion) {

        if (salesOrdersRequest.getSalesAgentPublicId() != null) {
            if (appVersion != null && !StringUtils.isEmpty(appVersion)) {
                versionHelper.checkIfAppVersionIsValid(appVersion, MobileUserType.SalesAgent);
            }
            agentActiveChecker.checkIfAgentIsActive(salesOrdersRequest.getSalesAgentPublicId(), AgentType.SALES_AGENT);
        }
        return salesOrdersService.placeOrder(salesOrdersRequest);
    }

    // fetch customer orders

    @JsonView(BaseView.SalesOrdersView.class)
    @GetMapping("/customer/orders")
    @Operation(summary = "Get Customer Orders", description = "Get Customer Orders",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<SalesOrders> getCustomerOrders(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "salesOrderNumber", required = false) String salesOrderNumber,
            @RequestParam(name = "warehouseId", required = false) UUID warehouseId,
            @RequestParam(name = "organizationRegionId", required = false) UUID organizationRegionId,
            @RequestParam(name = "salesOrderStatus", required = false) LoadingStatusEnum salesOrderStatus,
            @RequestParam(name = "paymentStatus", required = false) PaymentStatusEnum paymentStatus,
            @RequestParam(name = "invoiceNumber", required = false) String invoiceNumber,
            @RequestParam(name = "isCredit", required = false) Boolean isCredit,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @Parameter(hidden = true) Principal principal,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return salesOrdersService.getCustomerOrders(searchParam,
                salesOrderNumber,
                warehouseId,
                organizationRegionId,
                salesOrderStatus,
                paymentStatus,
                invoiceNumber,
                isCredit,
                startDate,
                endDate,
                principal.getName(),
                pageable);
    }

    // cancel sales order
    @PatchMapping("/{salesOrderPublicId}/cancel")
    @Operation(summary = "Cancel a Sales Order", description = "Cancel a Sales Order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void cancelSalesOrder(@PathVariable UUID salesOrderPublicId,
                                 @RequestParam(name = "isCustomerInitiated", required = false, defaultValue = "false") Boolean isCustomerInitiated,
                                 @RequestParam(name = "rejectionReasonPublicId", required = false) UUID rejectionReasonPublicId,
                                 @Parameter(hidden = true) Principal principal) {

        salesOrdersService.cancelSalesOrder(salesOrderPublicId, isCustomerInitiated, rejectionReasonPublicId, principal.getName());
    }

    /**
     * update this special discount so that a customer can have more than one approval even if the discount is active  @Reminder 9:30Pm
     *
     * @param specialRequest
     * @param principal
     */
    // request for special discount
    @PatchMapping("/requestSpecialDiscount")
    @Operation(summary = "Request for Special Discount on a Sales Order", description = "Request for Special Discount on a Sales Order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void requestForSpecialDiscount(@RequestBody SpecialRequest specialRequest,
                                          @Parameter(hidden = true) Principal principal) {

        salesOrdersService.requestForSpecialDiscount(specialRequest, principal.getName());
    }

    @PostMapping("/cartDiscountCheck")
    @Operation(summary = "Check for Discount Approval on a Sales Order Cart", description = "Check for Discount Approval on a Sales Order Cart",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
                    @ExampleObject(name = "CHECK CART DISCOUNT", value = Examples.CART_DISCOUNT_CHECK),
            })))
    public BigDecimal checkForDiscountApproval(@RequestBody CartDiscountRequest cartDiscountRequest,
                                               @Parameter(hidden = true) Principal principal) {

        return salesOrdersService.checkForDiscountApproval(cartDiscountRequest, principal.getName());
    }

    // recalculate discount

    @PatchMapping("/{salesOrderPublicId}/recalculateDiscount")
    @Operation(summary = "Recalculate Discount on a Sales Order", description = "Recalculate Discount on a Sales Order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void recalculateDiscount(@PathVariable UUID salesOrderPublicId,
                                    @Parameter(hidden = true) Principal principal) {

        salesOrdersService.recalculateDiscount(salesOrderPublicId, principal.getName());
    }

    //    @JsonView(BaseView.SalesOrdersView.class)
    @GetMapping("/salesOrderAgent")
    @Operation(
            summary = "Get all Sales Orders",
            description = "Get all Sales Orders",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<SalesOrderAgent> getSalesOrderAgent(
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return salesOrdersService.getSalesOrderAgent(pageable);
    }

    //get sales order stats

    @GetMapping("/salesOrderStats")
    @Operation(
            summary = "Get Sales Order Stats",
            description = "Get Sales Order Stats",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public OrderStatsDTO getSalesOrderStats(@RequestParam(name = "searchParam", required = false) String searchParam,
                                            @RequestParam(name = "salesOrderNumber", required = false) String salesOrderNumber,
                                            @RequestParam(name = "customerId", required = false) UUID customerId,
                                            @RequestParam(name = "warehouseId", required = false) UUID warehouseId,
                                            @RequestParam(name = "organizationRegionId", required = false) UUID organizationRegionId,
                                            @RequestParam(name = "locationId", required = false) UUID locationId,
                                            @RequestParam(name = "sourceChannel", required = false) SourceChannelEnum sourceChannel,
                                            @RequestParam(name = "salesOrderStatus", required = false) LoadingStatusEnum salesOrderStatus,
                                            @RequestParam(name = "statuses", required = false) List<LoadingStatusEnum> statuses,
                                            @RequestParam(name = "paymentStatus", required = false) PaymentStatusEnum paymentStatus,
                                            @RequestParam(name = "invoiceNumber", required = false) String invoiceNumber,
                                            @RequestParam(name = "bookedBySalesAgent", required = false) Boolean bookedBySalesAgent,
                                            @RequestParam(name = "salesAgentPublicId", required = false) UUID salesAgentPublicId,
                                            @RequestParam(name = "deliveryAgentPublicId", required = false) UUID deliveryAgentPublicId,
                                            @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
                                            @RequestParam(name = "isCredit", required = false) Boolean isCredit,
                                            @RequestParam(name = "isWalkIn", required = false) Boolean isWalkIn,
                                            @RequestParam(name = "bookedByParent", required = false) Boolean bookedByParent,
                                            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return salesOrdersService.getSalesOrderStats(searchParam,
                salesOrderNumber,
                customerId,
                warehouseId,
                organizationRegionId,
                sourceChannel,
                salesOrderStatus,
                paymentStatus,
                invoiceNumber,
                startDate,
                endDate,
                locationId,
                bookedBySalesAgent,
                salesAgentPublicId,
                deliveryAgentPublicId,
                parentCustomerPublicId,
                isCredit,
                statuses, isWalkIn, bookedByParent);
    }


    // recalculate tra receipt

    @PatchMapping("/{salesOrderPublicId}/resendTraInvoice")
    @Operation(summary = "Generate TRA Receipt for a Sales Order", description = "Generate TRA Receipt for a Sales Order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void generateTraReceipt(@PathVariable UUID salesOrderPublicId,
                                   @Parameter(hidden = true) Principal principal) {

        salesOrdersService.generateTraReceipt(salesOrderPublicId, principal.getName());
    }

    @PatchMapping("/{salesOrderPublicId}/convertCashToCredit")
    @Operation(summary = "Convert a delivered unpaid cash order to credit",
            description = "Admin correction path when goods were delivered but cash was not collected. "
                    + "Does not re-mark delivery; creates BNPL credit item and refreshes route recon when possible.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CONVERT_DELIVERED_CASH_TO_CREDIT", value = Examples.CONVERT_DELIVERED_CASH_TO_CREDIT),
    })))
    public void convertDeliveredCashOrderToCredit(@PathVariable UUID salesOrderPublicId,
                                                  @RequestBody @Valid ConvertCashToCreditRequest request,
                                                  @Parameter(hidden = true) Principal principal) {

        deliveryAgentsService.convertDeliveredCashOrderToCredit(
                salesOrderPublicId, request, principal.getName());
    }

    @JsonView(BaseView.SalesOrdersView.class)
    @GetMapping("/customerOrders/{customerPublicId}")
    @Operation(summary = "Get all Customer Sales Orders", description = "Get all Customer Sales Orders",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    @PageableAsQueryParam
    public Page<SalesOrders> getAllSalesOrders(
            @PathVariable UUID customerPublicId,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return salesOrdersService.getAllCustomerSalesOrders(
                customerPublicId,
                pageable
        );
    }


    @PatchMapping("/{publicId}/transitionOrders")
    @Operation(summary = "Transition Orders", description = "TransitionOrders",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void transitionOrders(@PathVariable(name = "publicId") UUID orderPublicId,
                                 @Parameter(hidden = true) Principal principal) {
        salesOrdersService.transitionOrders(orderPublicId, principal.getName());

    }

    @JsonView(BaseView.SalesOrderMiniView.class)
    @PostMapping("/walk-in-order")
    @Operation(summary = "Create a new Walk In Sales Order", description = "Create a new Walk In Sales Order",
            responses = {@ApiResponse(responseCode = "200", description = "OK")
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_SALES_ORDER", value = Examples.CREATE_WALK_IN_SALE_ORDER_request)})))
    public SalesOrders createWalkInSalesOrder(@RequestBody @Valid WalkInSalesOrdersRequest walkInSalesOrdersRequest,
                                              @Parameter(hidden = true) Principal principal) {

        return salesOrdersService.createWalkInSalesOrder(walkInSalesOrdersRequest, principal.getName());
    }

    @JsonView(BaseView.ProductReconView.class)
    @GetMapping("/walk-in-product-reconciliations")
    @Operation(summary = "Get all walk in product reconciliations", description = "Get all walk in product reconciliations",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    @PageableAsQueryParam
    public Page<ProductRecon> getWalkInProductReconciliations(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "reconciled", required = false) Boolean reconciled,
            @RequestParam(name = "warehousePublicId", required = false) String warehousePublicId,
            @RequestParam(name = "status", required = false) ReconStatus status,
            @RequestParam(name = "createdBy", required = false) UUID createdBy,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {

        return salesOrdersService.getWalkInProductReconciliations(
                searchParam, reconciled, warehousePublicId, status, createdBy, startDate, endDate,
                principal,
                pageable
        );
    }

    @PostMapping("/{salesOrderPublicId}/amend")
    @Operation(summary = "amend fully delivered Sales Order", description = "amend fully delivered Sales Order",
            responses = {@ApiResponse(responseCode = "200", description = "OK"),}, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_SALES_ORDER", value = Examples.Order_Fully_Delivered_Amendment_Request),})))
    public void amendOrder(@PathVariable UUID salesOrderPublicId,
                           @Valid @RequestBody AmendOrderItemRequest amendOrderItemRequest,
                           @Parameter(hidden = true) Principal principal) {

        deliveryAgentsService.amendFullDeliveredOrder(salesOrderPublicId, amendOrderItemRequest, principal);
    }

    @PostMapping("/{customerReturnPublicId}/approveAmendment")
    @Operation(summary = "approve amendment", description = "approve amendment",
            responses = {@ApiResponse(responseCode = "200", description = "OK"),}, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_SALES_ORDER", value = ""),})))
    public void approveCustomerReturn(@PathVariable UUID customerReturnPublicId,
                                      @RequestParam(name = "returnApprovalType") ReturnApprovalType returnApprovalType,
                                      @Parameter(hidden = true) Principal principal) {

        deliveryAgentsService.approveCustomerReturn(customerReturnPublicId, returnApprovalType, principal);
    }

    @GetMapping("/{deliveryAgentPublicId}/oderNumbers")
    @Operation(summary = "Get Sales Order for DA", description = "Get Sales Order for DA",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<String> getSalesOrderNumbers(@PathVariable UUID deliveryAgentPublicId,
                                             @Parameter(hidden = true) Principal principal) {

        return salesOrdersService.getSalesOrderNumbers(deliveryAgentPublicId, principal);
    }

    @JsonView(BaseView.SalesOrderItemsView.class)
    @GetMapping("/get-sales-items")
    @Operation(summary = "Get sales items by order number", description = "Get sales items by order number",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    @PageableAsQueryParam
    public CustomerReturnOrderItem getSalesOrderItemList(
            @RequestParam(name = "orderNumber") String orderNumber,
            @Parameter(hidden = true) Principal principal) {

        return salesOrdersService.getSalesOrderItemList(
                orderNumber,
                principal
        );
    }

    @JsonView(BaseView.CustomerReturnView.class)
    @GetMapping("/get-customer-returns")
    @Operation(summary = "Get sales items by order number", description = "Get sales items by order number",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    @PageableAsQueryParam
    public Page<CustomerReturn> getCustomerReturnList(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "deliveryAgentPublicId", required = false) UUID deliveryAgentPublicId,
            @RequestParam(name = "status", required = false) CustomerReturnStatus status,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "orderNumber", required = false) String orderNumber,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {

        return salesOrdersService.getCustomerReturnList(
                searchParam, orderNumber, deliveryAgentPublicId, status, startDate, endDate,
                principal,
                pageable
        );
    }

    @JsonView(BaseView.CustomerReturnView.class)
    @GetMapping("/get-customer-returns/{returnPublicId}/items")
    @Operation(summary = "Get sales items by order number", description = "Get sales items by order number",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    @PageableAsQueryParam
    public Page<CustomerReturnItem> getCustomerReturnItemList(
            @PathVariable UUID returnPublicId,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(hidden = true) Principal principal) {

        return salesOrdersService.getCustomerReturnItemList(
                returnPublicId,
                principal,
                pageable
        );
    }

    @GetMapping("/salesAgentOrders/{salesAgentPublicId}")
    @Operation(
            summary = "Get all Sales Orders for SA",
            description = "Get all Sales Orders for SA",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public SalesAgentOrderPageable getAllSalesOrdersForSA(@PathVariable String salesAgentPublicId,
                                                          @RequestParam(name = "searchParam", required = false) String searchParam,
                                                          @RequestParam(name = "salesOrderNumber", required = false) String salesOrderNumber,
                                                          @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                          @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                          @PageableDefault(size = 50, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return salesOrdersService.getAllSalesOrdersForSA(searchParam, salesOrderNumber, startDate, endDate, pageable, salesAgentPublicId);
    }

    @GetMapping("/bnplOrderSummary")
    @Operation(
            summary = "Get BNPL Order Summary",
            description = "Get BNPL Order Summary",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BNPLOrderSummary getBNPLOrderSummary(@RequestParam(name = "salesOrderPublicId", required = true) UUID salesOrderPublicId) {
        return salesOrdersService.getBNPLOrderSummary(salesOrderPublicId);
    }

}
