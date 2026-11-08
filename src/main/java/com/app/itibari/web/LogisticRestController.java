package com.app.itibari.web;

import com.app.itibari.dataTypes.*;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.OrderCutOffTimeRequest;
import com.app.itibari.models.request.RoutePlanningRequest;
import com.app.itibari.models.request.TransferInTransitRequest;
import com.app.itibari.models.request.VehicleRequest;
import com.app.itibari.models.response.RoutePlanningOrderSlimDTO;
import com.app.itibari.models.response.RouteProductsDTO;
import com.app.itibari.services.LogisticService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Tag(name = "Logistics", description = "Logistics API")
@RestController
@RequestMapping("/logistics")
public class LogisticRestController {

    private final LogisticService logisticService;

    public LogisticRestController(LogisticService logisticService) {

        this.logisticService = logisticService;
    }

    @PreAuthorize("hasAnyAuthority('CREATE_VEHICLE')")
    @JsonView(BaseView.VehicleView.class)
    @PostMapping("/vehicle")
    @Operation(summary = "Create a new vehicle", description = "Create a new vehicle",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_VEHICLE", value = Examples.CREATE_VEHICLE),
    })))
    public Vehicle createVehicle(@RequestBody @Valid VehicleRequest vehicleRequest,
                                 @Parameter(hidden = true) Principal principal) {

        return logisticService.createVehicle(vehicleRequest, principal.getName());
    }

    //update

    @PreAuthorize("hasAnyAuthority('UPDATE_VEHICLE')")
    @PutMapping("/vehicle/{vehiclePublicId}")
    @Operation(summary = "Update a vehicle", description = "Update a vehicle",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_VEHICLE", value = Examples.UPDATE_VEHICLE),
    })))
    public void updateVehicle(@PathVariable(name = "vehiclePublicId") UUID vehiclePublicId,
                              @RequestBody @Valid VehicleRequest vehicleRequest,
                              @Parameter(hidden = true) Principal principal) {

        logisticService.updateVehicle(vehiclePublicId, vehicleRequest, principal.getName());
    }

    // get

    @PreAuthorize("hasAnyAuthority('VIEW_VEHICLE')")
    @JsonView(BaseView.VehicleView.class)
    @GetMapping("/vehicle/{vehiclePublicId}")
    @Operation(summary = "Get a vehicle", description = "Get a vehicle",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Vehicle getVehicle(@PathVariable(name = "vehiclePublicId") UUID vehiclePublicId) {

        return logisticService.getVehicle(vehiclePublicId);
    }

    //get all

    @PreAuthorize("hasAnyAuthority('VIEW_VEHICLE')")
    @JsonView(BaseView.VehicleView.class)
    @GetMapping("/vehicles")
    @Operation(summary = "Get all vehicles", description = "Get all vehicles",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_VEHICLES", value = Examples.GET_VEHICLES)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<Vehicle> getVehicles(@RequestParam(required = false, name = "searchParam") String searchParam,
                                     @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return logisticService.getVehicles(searchParam, pageable);
    }

    //delete
    @PreAuthorize("hasAnyAuthority('DELETE_VEHICLE')")
    @DeleteMapping("/vehicle/{vehiclePublicId}")
    @Operation(summary = "Delete a vehicle", description = "Delete a vehicle",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteVehicle(@PathVariable(name = "vehiclePublicId") UUID vehiclePublicId,
                              @Parameter(hidden = true) Principal principal) {

        logisticService.deleteVehicle(vehiclePublicId, principal.getName());
    }

    /*
    cut off time
     */

    @PreAuthorize("hasAnyAuthority('CREATE_ORDER_CUT_OFF_TIME')")
    @JsonView(BaseView.VehicleView.class)
    @PatchMapping("/orderCutOffTime")
    @Operation(summary = "Create a new order cut off time", description = "Create a new order cut off time",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_ORDER_CUT_OFF_TIME", value = Examples.CREATE_ORDER_CUT_OFF_TIME),
    })))
    public OrderCutOffTime createOrderCutOffTime(@RequestBody @Valid OrderCutOffTimeRequest orderCutOffTimeRequest,
                                                 @Parameter(hidden = true) Principal principal) {

        return logisticService.createOrderCutOffTime(orderCutOffTimeRequest, principal.getName());
    }

    @PreAuthorize("hasAnyAuthority('VIEW_ORDER_CUT_OFF_TIME')")
    @JsonView(BaseView.VehicleView.class)
    @GetMapping("/orderCutOffTime")
    @Operation(summary = "Get all order cut off time", description = "Get all order cut off time",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_ORDER_CUT_OFF_TIME", value = Examples.GET_ORDER_CUT_OFF_TIME)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<OrderCutOffTime> getOrderCutOffTime(@RequestParam(required = false, name = "searchParam") String searchParam,
                                                    @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return logisticService.getOrderCutOffTime(searchParam, pageable);
    }

    @PreAuthorize("hasAnyAuthority('DELETE_ORDER_CUT_OFF_TIME')")
    @DeleteMapping("/orderCutOffTime/{orderCutOffTimePublicId}")
    @Operation(summary = "Delete a order cut off time", description = "Delete a order cut off time",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteOrderCutOffTime(@PathVariable(name = "orderCutOffTimePublicId") UUID orderCutOffTimePublicId,
                                      @Parameter(hidden = true) Principal principal) {

        logisticService.deleteOrderCutOffTime(orderCutOffTimePublicId, principal.getName());
    }

    /*
    Route Planning
     */

    @PreAuthorize("hasAnyAuthority('CREATE_ROUTE_PLANNING')")
    @JsonView(BaseView.RouteView.class)
    @PostMapping("/routePlanning")
    @Operation(summary = "Create a new route planning", description = "Create a new route planning",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_ROUTE_PLANNING", value = Examples.CREATE_ROUTE_PLANNING),
    })))
    public RoutePlanning createRoutePlanning(@RequestBody @Valid RoutePlanningRequest routePlanningRequest,
                                             @Parameter(hidden = true) Principal principal) {

        return logisticService.createRoutePlanning(routePlanningRequest, principal.getName());
    }

    @PreAuthorize("hasAnyAuthority('CREATE_ROUTE_PLANNING')")
    @JsonView(BaseView.RouteView.class)
    @PostMapping("/routeWrapper/action")
    @Operation(summary = "Start route wrapper", description = "Start route wrapper for today's route planning",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public RouteWrapper startRouteWrapper(@RequestParam(name = "action") RouteWrapperAction wrapperAction,
                                          @Parameter(hidden = true) Principal principal) {

        return logisticService.routeWrapperAction(wrapperAction, principal.getName());
    }

    @PreAuthorize("hasAnyAuthority('VIEW_ROUTE_PLANNING')")
    @JsonView(BaseView.RouteView.class)
    @GetMapping("/routeWrappers")
    @Operation(summary = "Get all route wrappers", description = "Get all route wrappers",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<RouteWrapper> getRouteWrappers(
            @RequestParam(required = false, name = "searchParam") String searchParam,
            @RequestParam(required = false, name = "from") String from,
            @RequestParam(required = false, name = "to") String to,
            @RequestParam(required = false, name = "date") String date,
            @RequestParam(required = false, name = "status") WrapperStatus status,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return logisticService.getRouteWrappers(searchParam, from, to, date, status, pageable);
    }

    @PreAuthorize("hasAnyAuthority('VIEW_ROUTE_PLANNING')")
    @JsonView(BaseView.RouteView.class)
    @GetMapping("/routePlanning/{routePlanningPublicId}")
    @Operation(summary = "Get a route planning", description = "Get a route planning",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public RoutePlanning getRoutePlanning(@PathVariable(name = "routePlanningPublicId") UUID routePlanningPublicId) {

        return logisticService.getRoutePlanning(routePlanningPublicId);
    }

    @PreAuthorize("hasAnyAuthority('VIEW_ROUTE_PLANNING')")
    @JsonView(BaseView.RouteView.class)
    @GetMapping("/routePlannings")
    @Operation(summary = "Get all route plannings", description = "Get all route plannings",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_ROUTE_PLANNINGS", value = Examples.GET_ROUTE_PLANNINGS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<RoutePlanning> getRoutePlannings(
            @RequestParam(required = false, name = "searchParam") String searchParam,
            @RequestParam(required = false, name = "vehiclePublicId") String vehiclePublicId,
            @RequestParam(required = false, name = "warehousePublicId") String warehousePublicId,
            @RequestParam(required = false, name = "deliveryAgentPublicId") String deliveryAgentPublicId,
            @RequestParam(required = false, name = "routeWrapperPublicId") UUID routeWrapperPublicId,
            @RequestParam(required = false, name = "region") String region,
            @RequestParam(required = false, name = "from") String from,
            @RequestParam(required = false, name = "to") String to,
            @RequestParam(required = false, name = "date") String date,
            @RequestParam(required = false, name = "status") RouteStatus status,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return logisticService.getRoutePlannings(searchParam,
                vehiclePublicId,
                warehousePublicId,
                deliveryAgentPublicId,
                routeWrapperPublicId,
                region,
                from,
                to,
                status,
                date,
                pageable);
    }

    @PreAuthorize("hasAnyAuthority('DELETE_ROUTE_PLANNING')")
    @DeleteMapping("/routePlanning/{routePlanningPublicId}")
    @Operation(summary = "Delete a route planning", description = "Delete a route planning",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteRoutePlanning(@PathVariable(name = "routePlanningPublicId") UUID routePlanningPublicId,
                                    @Parameter(hidden = true) Principal principal) {

        logisticService.deleteRoutePlanning(routePlanningPublicId, principal.getName());
    }

    @Deprecated
    @PreAuthorize("hasAnyAuthority('UPDATE_ROUTE_PLANNING')")
    @PatchMapping("/routePlanning/{routePlanningPublicId}/actions")
    @Operation(summary = "Add action to route planning", description = "Add action to route planning",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "ADD_ROUTE_PLANNING_ACTION", value = Examples.ADD_ROUTE_PLANNING_ACTION),
            @ExampleObject(name = "TRANSFER_PLANNING", value = Examples.TRANFER_PLANNING),})))
    public void addActionToRoutePlanning(@PathVariable(name = "routePlanningPublicId") UUID routePlanningPublicId,
                                         @RequestBody @Valid RoutePlanningRequest routePlanningRequest,
                                         @Parameter(hidden = true) Principal principal) {

        logisticService.addActionToRoutePlanning(routePlanningPublicId, routePlanningRequest, principal.getName());
    }

    // route orders

    @PreAuthorize("hasAnyAuthority('VIEW_ROUTE_ORDER')")
    @JsonView(BaseView.RouteOrderView.class)
    @GetMapping("/routePlanning/{routePlanningPublicId}/orders")
    @Operation(summary = "Get all orders in a route planning", description = "Get all orders in a route planning",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_ROUTE_PLANNING_ORDERS", value = Examples.GET_ROUTE_PLANNING_ORDERS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<SalesOrders> getRoutePlanningOrders(@PathVariable UUID routePlanningPublicId,
                                                    @RequestParam(name = "searchParam", required = false) String searchParam,
                                                    @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return logisticService.getRoutePlanningOrders(routePlanningPublicId, searchParam, pageable);
    }

    // route planning orders and van sales products items

    @PreAuthorize("hasAnyAuthority('VIEW_ROUTE_ORDER')")
    @GetMapping("/routePlanning/{routePlanningPublicId}/loadingProducts")
    @Operation(summary = "Get all orders in a route planning", description = "Get all orders in a route planning",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_ROUTE_PRODUCT_ITEMS", value = Examples.GET_ROUTE_PLANNING_PRODUCT_LOADING_ITEMS)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ResponseEntity<RouteProductsDTO> getRoutePlanningOrdersAndVanSalesProducts(@PathVariable(name = "routePlanningPublicId") UUID routePlanningPublicId,
                                                                                      @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        RouteProductsDTO routeProductsDTO = logisticService.getRoutePlanningOrdersAndVanSalesProducts(routePlanningPublicId, pageable, null);
        return ResponseEntity.ok(routeProductsDTO);
    }

    @PreAuthorize("hasAnyAuthority('VIEW_ROUTE_ORDER')")
    @GetMapping("/routePlanning/{dispatchId}/orders/slim")
    @Operation(summary = "Get orders in a dispatch (slim)", description = "Returns a reduced payload with only essential order and customer fields",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<RoutePlanningOrderSlimDTO> getRoutePlanningOrdersSlim(@PathVariable(name = "dispatchId") UUID dispatchId,
                                                                      @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return logisticService.getRoutePlanningOrdersSlim(dispatchId, pageable);
    }

    /**
     * Transfer an entire route OR specific orders to another Delivery Agent while the
     * source route is in a post-loading / in-transit state (e.g. vehicle breakdown).
     *
     * <ul>
     *   <li><b>transferType = ROUTE</b> – moves ALL orders on the source route.</li>
     *   <li><b>transferType = ORDER</b> – moves only the orders listed in {@code orderPublicIds}.</li>
     * </ul>
     * <p>
     * In both cases the following bookkeeping is performed:
     * ProductRecon agent info is updated / a new ProductRecon is created,
     * ProductItems are split (ORDER mode), Reconciliation records are updated,
     * stock movements are recorded, and DA/vehicle availability flags are adjusted.
     */
    @PreAuthorize("hasAnyAuthority('UPDATE_ROUTE_PLANNING')")
    @PostMapping("/routePlanning/transfer-in-transit")
    @Operation(
            summary = "Transfer route or orders to another DA (in-transit / breakdown)",
            description = "Transfers a full route (ROUTE) or individual orders (ORDER) to a new Delivery Agent " +
                    "while the source route is still in a Loading, Loaded, ReadyToGo, or IN_TRANSIT state. " +
                    "Both transfer types merge into the target agent's active route when one exists; " +
                    "otherwise a new route is created. Multiple active target routes must be resolved before transferring. " +
                    "Handles ProductRecon, ProductItem, Reconciliation and stock-movement bookkeeping.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transfer completed"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "TRANSFER_IN_TRANSIT_ROUTE", value = Examples.TRANSFER_IN_TRANSIT_ROUTE),
            @ExampleObject(name = "TRANSFER_IN_TRANSIT_ORDER", value = Examples.TRANSFER_IN_TRANSIT_ORDER),
    })))
    public ResponseEntity<Void> transferOrderInTransit(@RequestBody @Valid TransferInTransitRequest request,
                                                       @Parameter(hidden = true) Principal principal) {

        log.info("=====Received transfer request=====: {}", request);
        logisticService.transferOrderInTransit(request, principal.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/issuances")
    @Operation(summary = "Get issuance", description = "Get issuance",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<Issuance> getIssuance(@PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return logisticService.getIssuance(pageable);
    }

    @GetMapping("/operationTimingLogs")
    @Operation(summary = "Get route and issuance timing logs", description = "Get route and issuance timing logs",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<OperationTimingLog> getOperationTimingLogs(
            @RequestParam(required = false, name = "entityType") OperationTimingEntityType entityType,
            @RequestParam(required = false, name = "status") OperationTimingStatus status,
            @RequestParam(required = false, name = "routeId") String routeId,
            @RequestParam(required = false, name = "routePublicId") String routePublicId,
            @RequestParam(required = false, name = "date") String date,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return logisticService.getOperationTimingLogs(entityType, status, routeId, routePublicId, date, pageable);
    }

    @GetMapping("/delivery-operations")
    @Operation(summary = "Get delivery-operations", description = "Get delivery-operations",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public Page<DeliveryAgentOperation> getDeliveryAgentOperation(@PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return logisticService.getDeliveryAgentOperation(pageable);
    }
}
