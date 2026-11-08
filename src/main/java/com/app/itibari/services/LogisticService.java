package com.app.itibari.services;

import com.app.itibari.dataTypes.OperationTimingEntityType;
import com.app.itibari.dataTypes.OperationTimingStatus;
import com.app.itibari.dataTypes.RouteStatus;
import com.app.itibari.dataTypes.RouteWrapperAction;
import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.dataTypes.WrapperStatus;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.OrderCutOffTimeRequest;
import com.app.itibari.models.request.RoutePlanningRequest;
import com.app.itibari.models.request.TransferInTransitRequest;
import com.app.itibari.models.request.VehicleRequest;
import com.app.itibari.models.response.LiveStockPageable;
import com.app.itibari.models.response.RoutePlanningOrderSlimDTO;
import com.app.itibari.models.response.RouteProductsDTO;
import com.itextpdf.io.source.ByteArrayOutputStream;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface LogisticService {

    Vehicle createVehicle(@Valid VehicleRequest vehicleRequest, String name);

    void updateVehicle(UUID vehiclePublicId, @Valid VehicleRequest vehicleRequest, String name);

    Vehicle getVehicle(UUID vehiclePublicId);

    Page<Vehicle> getVehicles(String searchParam, Pageable pageable);

    void deleteVehicle(UUID vehiclePublicId, String name);

    OrderCutOffTime createOrderCutOffTime(@Valid OrderCutOffTimeRequest orderCutOffTimeRequest, String name);

    Page<OrderCutOffTime> getOrderCutOffTime(String searchParam, Pageable pageable);

    void deleteOrderCutOffTime(UUID orderCutOffTimePublicId, String name);

    RoutePlanning createRoutePlanning(@Valid RoutePlanningRequest routePlanningRequest, String name);

    RouteWrapper startRouteWrapper(String name);

    RouteWrapper routeWrapperAction(RouteWrapperAction wrapperAction, String name);

    Page<RouteWrapper> getRouteWrappers(String searchParam,
                                        String from,
                                        String to,
                                        String date,
                                        WrapperStatus status,
                                        Pageable pageable);

    RoutePlanning getRoutePlanning(UUID routePlanningPublicId);

    void deleteRoutePlanning(UUID routePlanningPublicId, String name);

    @Deprecated
    void addActionToRoutePlanning(UUID routePlanningPublicId, @Valid RoutePlanningRequest routePlanningRequest, String name);

    Page<SalesOrders> getRoutePlanningOrders(UUID routePlanningPublicId, String searchParam, Pageable pageable);

    Page<RoutePlanningOrderSlimDTO> getRoutePlanningOrdersSlim(UUID routePlanningPublicId, Pageable pageable);

    Page<RoutePlanning> getRoutePlannings(String searchParam,
                                          String vehiclePublicId,
                                          String warehousePublicId,
                                          String deliveryAgentPublicId,
                                          UUID routeWrapperPublicId,
                                          String region,
                                          String from,
                                          String to,
                                          RouteStatus status,
                                          String date,
                                          Pageable pageable);

    RouteProductsDTO getRoutePlanningOrdersAndVanSalesProducts(UUID routePlanningPublicId, Pageable pageable, Person person);

    LiveStockPageable getLiveStockReport(LocalDate localDate, UUID warehouseId, String productName, Pageable pageable);

    ByteArrayOutputStream generateStaticReportExport(LocalDate reportDate, UUID warehouseId, String productName, Pageable pageable, StatementExportType statementExportType, String name);

    /**
     * Transfer an entire route (ROUTE) or individual orders (ORDER) to another Delivery Agent
     * while the source route is in a Loading, Loaded, or IN_TRANSIT state.
     * Handles RoutePlanning, ProductRecon, ProductItem and Reconciliation bookkeeping.
     */
    void transferOrderInTransit(@Valid TransferInTransitRequest request, String name);

    Page<Issuance> getIssuance(Pageable pageable);

    Page<DeliveryAgentOperation> getDeliveryAgentOperation(Pageable pageable);

    Page<OperationTimingLog> getOperationTimingLogs(OperationTimingEntityType entityType,
                                                    OperationTimingStatus status,
                                                    String routeId,
                                                    String routePublicId,
                                                    String date,
                                                    Pageable pageable);
}
