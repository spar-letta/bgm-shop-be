package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.dataTypes.Module;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.OrderCutOffTimeRequest;
import com.app.itibari.models.request.RoutePlanningRequest;
import com.app.itibari.models.request.TransferInTransitRequest;
import com.app.itibari.models.request.VehicleRequest;
import com.app.itibari.models.response.*;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.LogisticsHelper;
import com.app.itibari.services.helper.StockMovementHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.TimingLogHelper;
import com.app.itibari.utils.helpers.Validators;
import com.app.itibari.utils.reportHelper.StaticReport;
import com.app.itibari.utils.specification.OrderCutOffSpecification;
import com.app.itibari.utils.specification.RoutePlanningSpecification;
import com.app.itibari.utils.specification.RouteWrapperSpecification;
import com.app.itibari.utils.specification.VehicleSpecification;
import com.itextpdf.io.source.ByteArrayOutputStream;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticServiceImpl implements LogisticService {

    private final VehicleRepository vehicleRepository;

    private final PersonRepository personRepository;

    private final OrderCutOffTimeRepository orderCutOffTimeRepository;

    private final WarehouseRepository warehouseRepository;

    private final OrganizationRegionsRepository organizationRegionsRepository;

    private final CounterService counterService;

    private final SalesOrdersRepository salesOrdersRepository;

    private final DeliveryAgentsRepository deliveryAgentsRepository;

    private final RoutePlanningRepository routePlanningRepository;

    private final RouteWrapperRepository routeWrapperRepository;

    private final LogisticsHelper logisticsHelper;

    private final VanSalesRepository vanSalesRepository;

    private final LiveStockEntityRepository liveStockEntityRepository;
    private final StaticReport staticReport;
    private final StockMovementHelper stockMovementHelper;

    // --- added for in-transit transfer ---
    private final ProductReconRepository productReconRepository;
    private final ProductItemRepository productItemRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final RejectionReasonRepository rejectionReasonRepository;
    private final RejectionCodeRepository rejectionCodeRepository;
    private final IssuanceRepository issuanceRepository;
    private final DeliveryAgentOperationRepository deliveryAgentOperationRepository;
    private final EventsRepository eventsRepository;
    private final ItibariSmsService itibariSmsService;
    private final TimingLogHelper timingLogHelper;
    private final OperationTimingLogRepository operationTimingLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Vehicle createVehicle(VehicleRequest vehicleRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<Vehicle> optionalVehicle = vehicleRepository.findVehicleByNumberPlateIgnoreCaseAndDeletedIsFalse(vehicleRequest.getNumberPlate());

        if (optionalVehicle.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Vehicle already exists")
                    .userMessage("Vehicle already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setNumberPlate(vehicleRequest.getNumberPlate());
        vehicle.setOwner(vehicleRequest.getOwnerName());
        vehicle.setAssignedDriver(vehicleRequest.getAssignedDriver());
        vehicle.setRegistrationStatus(vehicleRequest.getRegistrationStatus());
        vehicle.setOwnerPhone(vehicleRequest.getOwnerPhoneNumber());
        vehicle.setInsuranceExpiryDate(vehicleRequest.getInsuranceExpiryDate());
        optionalPerson.ifPresent(vehicle::setCreatedBy);
        vehicle.setCreationTime(LocalDateTime.now());
        vehicle.setVehicleType(vehicleRequest.getVehicleType());
        vehicle.setWeight(vehicleRequest.getWeight());

        vehicle.setHeight(vehicleRequest.getHeight());
        vehicle.setWidth(vehicleRequest.getWidth());
        vehicle.setLength(vehicleRequest.getLength());
        vehicle.setPayloadCapacity(vehicleRequest.getPayloadCapacity());

        return vehicleRepository.save(vehicle);
    }

    @Override
    public void updateVehicle(UUID vehiclePublicId, VehicleRequest vehicleRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<Vehicle> optionalVehicle = vehicleRepository.findByPublicIdAndDeletedIsFalse(vehiclePublicId);

        if (optionalVehicle.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Vehicle not found")
                    .userMessage("Vehicle not found");
        }

        Vehicle vehicle = optionalVehicle.get();

        if (!vehicle.getNumberPlate().equalsIgnoreCase(vehicleRequest.getNumberPlate())) {
            Optional<Vehicle> optionalVehicle1 = vehicleRepository.findVehicleByNumberPlateIgnoreCaseAndDeletedIsFalse(vehicleRequest.getNumberPlate());

            if (optionalVehicle1.isPresent()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Vehicle already exists")
                        .userMessage("Vehicle already exists");
            }
        }

        vehicle.setNumberPlate(vehicleRequest.getNumberPlate());
        vehicle.setOwner(vehicleRequest.getOwnerName());
        vehicle.setAssignedDriver(vehicleRequest.getAssignedDriver());
        vehicle.setRegistrationStatus(vehicleRequest.getRegistrationStatus());
        vehicle.setOwnerPhone(vehicleRequest.getOwnerPhoneNumber());
        vehicle.setInsuranceExpiryDate(vehicleRequest.getInsuranceExpiryDate());
        optionalPerson.ifPresent(vehicle::setUpdatedBy);
        vehicle.setUpdateDate(LocalDateTime.now());
        vehicle.setVehicleType(vehicleRequest.getVehicleType());
        vehicle.setWeight(vehicleRequest.getWeight());

        vehicle.setHeight(vehicleRequest.getHeight());
        vehicle.setWidth(vehicleRequest.getWidth());
        vehicle.setLength(vehicleRequest.getLength());
        vehicle.setPayloadCapacity(vehicleRequest.getPayloadCapacity());

        vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle getVehicle(UUID vehiclePublicId) {

        Optional<Vehicle> optionalVehicle = vehicleRepository.findByPublicIdAndDeletedIsFalse(vehiclePublicId);

        if (optionalVehicle.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Vehicle not found")
                    .userMessage("Vehicle not found");
        }

        return optionalVehicle.get();
    }

    @Override
    public Page<Vehicle> getVehicles(String searchParam, Pageable pageable) {

        return vehicleRepository.findAll(VehicleSpecification.isNotDeleted()
                .and(VehicleSpecification.searchParamMatches(searchParam)), pageable);
    }

    @Override
    public void deleteVehicle(UUID vehiclePublicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<Vehicle> optionalVehicle = vehicleRepository.findByPublicIdAndDeletedIsFalse(vehiclePublicId);

        if (optionalVehicle.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Vehicle not found")
                    .userMessage("Vehicle not found");
        }

        Vehicle vehicle = optionalVehicle.get();
        optionalPerson.ifPresent(vehicle::setUpdatedBy);
        vehicle.setDeleted(true);
        vehicle.setUpdateDate(LocalDateTime.now());

        vehicleRepository.save(vehicle);
    }

    @Override
    public OrderCutOffTime createOrderCutOffTime(OrderCutOffTimeRequest orderCutOffTimeRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        OrderCutOffTime orderCutOffTime = orderCutOffTimeRepository.findOrderCutOffTimeByDeletedIsFalse().orElse(new OrderCutOffTime());

        orderCutOffTime.setWarehouse(null);
        orderCutOffTime.setCutOffTime(orderCutOffTimeRequest.getCutOffTime());
        optionalPerson.ifPresent(orderCutOffTime::setCreatedBy);
        optionalPerson.ifPresent(orderCutOffTime::setUpdatedBy);
        orderCutOffTime.setCreationTime(LocalDateTime.now());
        orderCutOffTime.setUpdateDate(LocalDateTime.now());
        orderCutOffTime.setCutOffTimeEnd(orderCutOffTimeRequest.getCutOffTimeEnd());

        return orderCutOffTimeRepository.save(orderCutOffTime);
    }

    @Override
    public Page<OrderCutOffTime> getOrderCutOffTime(String searchParam, Pageable pageable) {

        return orderCutOffTimeRepository.findAll(OrderCutOffSpecification.isNotDeleted()
                .and(OrderCutOffSpecification.searchParamMatches(searchParam)), pageable);
    }

    @Override
    public void deleteOrderCutOffTime(UUID orderCutOffTimePublicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<OrderCutOffTime> optionalOrderCutOffTime = orderCutOffTimeRepository.findByPublicIdAndDeletedIsFalse(
                orderCutOffTimePublicId);

        if (optionalOrderCutOffTime.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Order cut off time not found")
                    .userMessage("Order cut off time not found");
        }

        OrderCutOffTime orderCutOffTime = optionalOrderCutOffTime.get();
        optionalPerson.ifPresent(orderCutOffTime::setUpdatedBy);
        orderCutOffTime.setDeleted(true);
        orderCutOffTime.setUpdateDate(LocalDateTime.now());

        orderCutOffTimeRepository.save(orderCutOffTime);
    }

    @Override
    public RoutePlanning createRoutePlanning(RoutePlanningRequest routePlanningRequest, String name) {
        return doCreateRoutePlanning(routePlanningRequest, name);
    }

    private RoutePlanning doCreateRoutePlanning(RoutePlanningRequest routePlanningRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (Validators.anyEqualNull(routePlanningRequest.getVehiclePublicId(),
                routePlanningRequest.getWarehousePublicId(),
                routePlanningRequest.getDeliveryAgentPublicId(),
                routePlanningRequest.getRegionPublicId(),
                routePlanningRequest.getSalesOrdersPublicIds())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Vehicle, Warehouse, Delivery Agent, Region, Route Wrapper and Sales Orders are required")
                    .userMessage("Vehicle, Warehouse, Delivery Agent, Region, Route Wrapper and Sales Orders are required");
        }

        Optional<Vehicle> optionalVehicle = vehicleRepository.findByPublicIdAndDeletedIsFalse(routePlanningRequest.getVehiclePublicId());
        Optional<Warehouse> optionalWarehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(routePlanningRequest.getWarehousePublicId());
        Optional<DeliveryAgents> optionalDeliveryAgent = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(routePlanningRequest.getDeliveryAgentPublicId());
        Optional<OrganizationRegions> optionalRegion = organizationRegionsRepository.findByPublicIdAndDeletedIsFalse(routePlanningRequest.getRegionPublicId());

        RouteWrapper routeWrapper = null;

        if (routePlanningRequest.getRouteWrapperPublicId() != null) {
            Optional<RouteWrapper> optionalRouteWrapper = routeWrapperRepository.findByPublicIdAndDeletedIsFalse(routePlanningRequest.getRouteWrapperPublicId());
            if (optionalRouteWrapper.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Route wrapper not found")
                        .userMessage("Route wrapper not found");
            }
            routeWrapper = optionalRouteWrapper.get();

            if (!isToday(routeWrapper.getCreationTime())) {
                throw new BadRequestRestApiException()
                        .developerMessage("Route wrapper was not created today")
                        .userMessage("Route wrapper was not created today");
            }
        }

        if (optionalVehicle.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Vehicle not found")
                    .userMessage("Vehicle not found");
        }

        if (optionalWarehouse.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse not found")
                    .userMessage("Warehouse not found");
        }

        if (optionalDeliveryAgent.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery Agent not found")
                    .userMessage("Delivery Agent not found");
        }

        if (optionalRegion.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Region not found")
                    .userMessage("Region not found");
        }

        // check if agent has active route

        Warehouse warehouse = optionalWarehouse.get();
        Vehicle vehicle = optionalVehicle.get();
        DeliveryAgents deliveryAgent = optionalDeliveryAgent.get();
        OrganizationRegions region = optionalRegion.get();


        if (!deliveryAgent.isAvailable()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery Agent is not available")
                    .userMessage("Delivery Agent is not available");
        }

        List<RoutePlanning> routePlannings = routePlanningRepository.findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusesAndDeletedIsFalse(
                deliveryAgent.getPublicId().toString(),
                List.of(RouteStatus.Loading, RouteStatus.CREATED));
        //List<RoutePlanning> route = routePlanningRepository.findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusAndDeletedIsFalse(deliveryAgent.getPublicId().toString(), RouteStatus.Loading);

        if (!routePlannings.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery Agent has active route")
                    .userMessage("Delivery Agent has active route");
        }

        List<SalesOrders> salesOrders = routePlanningRequest.getSalesOrdersPublicIds().stream()
                .map(salesOrderPublicId -> {
                    Optional<SalesOrders> optionalSalesOrders = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrderPublicId);
                    if (optionalSalesOrders.isEmpty()) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Sales Order not found")
                                .userMessage("Sales Order not found");
                    }
                    return optionalSalesOrders.get();
                }).toList();


        List<Warehouse> warehouses = salesOrders.stream()
                .map(SalesOrders::getWarehouse)
                .distinct()
                .toList();

        if (warehouses.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales orders must be from a warehouse")
                    .userMessage("Sales orders must be from a warehouse");
        }

        if (warehouses.size() > 1) {
            throw new BadRequestRestApiException()
                    .developerMessage("All sales orders must be from the same warehouse")
                    .userMessage("All sales orders must be from the same warehouse");
        }

        if (warehouses.get(0).getPublicId() != warehouse.getPublicId()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales orders must be from the selected warehouse")
                    .userMessage("Sales orders must be from the selected warehouse");
        }

        BigDecimal totalWeight = salesOrders.stream()
                .map(SalesOrders::getTotalSalesOrderWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        RoutePlanning routePlanning = new RoutePlanning();

        routePlanning.setVehicleName(vehicle.getNumberPlate());
        routePlanning.setVehicleId(vehicle.getPublicId().toString());
        routePlanning.setVehicleWeight(String.valueOf(vehicle.getWeight()));
        routePlanning.setWarehouseName(warehouse.getWarehouseName());
        routePlanning.setWarehouseId(warehouse.getPublicId().toString());
        routePlanning.setDeliveryAgentName(deliveryAgent.getFullName());
        routePlanning.setDeliveryAgentId(deliveryAgent.getPublicId().toString());
        routePlanning.setRouteId("RT-" + counterService.getNextCounter(CounterType.ROUTE));
        routePlanning.setRouteRegionId(region.getPublicId().toString());
        routePlanning.setRouteRegionName(region.getName());
        routePlanning.setRouteWrapper(routeWrapper);
        routePlanning.setSalesOrders(salesOrders);
        optionalPerson.ifPresent(routePlanning::setCreatedBy);
        optionalPerson.ifPresent(routePlanning::setUpdatedBy);
        routePlanning.setCreationTime(LocalDateTime.now());
        routePlanning.setRouteStatus(RouteStatus.CREATED);

        RoutePlanning saved = routePlanningRepository.save(routePlanning);

        if (saved.getRouteWrapper() != null) {
            eventsRepository.save(new Events(saved.getPublicId().toString(),
                    "ROUTE CREATED",
                    LocalDateTime.now(),
                    "Route " + saved.getRouteId() + " created under wrapper " + saved.getRouteWrapper().getRouteWrapperId(),
                    "CREATE",
                    optionalPerson.orElse(null)));
        }

        saved.getSalesOrders().forEach(salesOrder -> {
            salesOrder.setDeliveryAgent(deliveryAgent);
            salesOrder.setSalesOrderStatus(LoadingStatusEnum.PENDING_LOADING);
            salesOrder.setRoutePublicId(saved.getPublicId().toString());

            salesOrdersRepository.save(salesOrder);
        });
        logisticsHelper.updateVehicleAvailability(vehicle.getPublicId(), false);
        return saved;
    }

    @Override
    public RouteWrapper startRouteWrapper(String name) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfDay.plusDays(1);

        Optional<RouteWrapper> existingRouteWrapper = routeWrapperRepository
                .findFirstByCreationTimeBetweenAndDeletedIsFalseOrderByCreationTimeAsc(startOfDay, startOfTomorrow);

        if (existingRouteWrapper.isPresent()) {
            return existingRouteWrapper.get();
        }

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        RouteWrapper routeWrapper = new RouteWrapper();
        routeWrapper.setRouteWrapperId("RW-" + counterService.getNextCounter(CounterType.ROUTE_WRAPPER));
        routeWrapper.setStart(now);
        routeWrapper.setWrapperStatus(WrapperStatus.InProgress);
        optionalPerson.ifPresent(routeWrapper::setCreatedBy);
        optionalPerson.ifPresent(routeWrapper::setUpdatedBy);
        routeWrapper.setCreationTime(now);

        RouteWrapper saved = routeWrapperRepository.save(routeWrapper);

        eventsRepository.save(new Events(saved.getPublicId().toString(),
                "ROUTE WRAPPER CREATED",
                LocalDateTime.now(),
                "Route wrapper " + saved.getRouteWrapperId() + " created",
                "CREATE",
                optionalPerson.orElse(null)));

        return saved;
    }

    @Override
    public RouteWrapper routeWrapperAction(RouteWrapperAction wrapperAction, String name) {

        if (wrapperAction == RouteWrapperAction.Start) {
            return startRouteWrapper(name);
        }

        if (wrapperAction == RouteWrapperAction.Complete) {
            return completeRouteWrapper(name);
        }

        throw new BadRequestRestApiException()
                .developerMessage("Route wrapper action not supported")
                .userMessage("Route wrapper action not supported");
    }

    private RouteWrapper completeRouteWrapper(String name) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfDay.plusDays(1);

        RouteWrapper routeWrapper = routeWrapperRepository
                .findFirstByCreationTimeBetweenAndDeletedIsFalseOrderByCreationTimeAsc(startOfDay, startOfTomorrow)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Route wrapper not found for today")
                        .userMessage("Route wrapper not found for today"));

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        routeWrapper.setEnd(now);
        routeWrapper.setWrapperStatus(WrapperStatus.Completed);
        optionalPerson.ifPresent(routeWrapper::setUpdatedBy);

        RouteWrapper saved = routeWrapperRepository.save(routeWrapper);

        eventsRepository.save(new Events(saved.getPublicId().toString(),
                "ROUTE WRAPPER COMPLETED",
                LocalDateTime.now(),
                "Route wrapper " + saved.getRouteWrapperId() + " completed",
                "COMPLETE",
                optionalPerson.orElse(null)));

        return saved;
    }

    private boolean isToday(LocalDateTime dateTime) {
        return dateTime != null && dateTime.toLocalDate().isEqual(LocalDate.now());
    }

    @Override
    public Page<RouteWrapper> getRouteWrappers(String searchParam,
                                               String from,
                                               String to,
                                               String date,
                                               WrapperStatus status,
                                               Pageable pageable) {

        return routeWrapperRepository.findAll(RouteWrapperSpecification.isNotDeleted()
                .and(RouteWrapperSpecification.hasDateBetween(from, to))
                .and(RouteWrapperSpecification.hasDate(date))
                .and(RouteWrapperSpecification.hasStatus(status))
                .and(RouteWrapperSpecification.hasRouteWrapperId(searchParam)), pageable);
    }

    private String routePlanningOperationName(RoutePlanningRequest routePlanningRequest) {
        if (routePlanningRequest == null) {
            return "Create route planning";
        }

        int ordersCount = routePlanningRequest.getSalesOrdersPublicIds() == null
                ? 0
                : routePlanningRequest.getSalesOrdersPublicIds().size();

        return String.format(
                "Create route planning deliveryAgent=%s vehicle=%s warehouse=%s orders=%d",
                routePlanningRequest.getDeliveryAgentPublicId(),
                routePlanningRequest.getVehiclePublicId(),
                routePlanningRequest.getWarehousePublicId(),
                ordersCount
        );
    }


    @Override
    public RoutePlanning getRoutePlanning(UUID routePlanningPublicId) {

        Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.getRoutePlanningByPublicIdAndDeletedIsFalse(
                routePlanningPublicId);

        if (optionalRoutePlanning.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route Planning not found")
                    .userMessage("Route Planning not found");
        }
        return optionalRoutePlanning.get();
    }

    @Override
    public Page<RoutePlanning> getRoutePlannings(String searchParam,
                                                 String vehiclePublicId,
                                                 String warehousePublicId,
                                                 String deliveryAgentPublicId,
                                                 UUID routeWrapperPublicId,
                                                 String region,
                                                 String from,
                                                 String to,
                                                 RouteStatus status,
                                                 String date,
                                                 Pageable pageable) {

        return routePlanningRepository.findAll(RoutePlanningSpecification.isNotDeleted()
                .and(RoutePlanningSpecification.hasWarehouse(warehousePublicId))
                .and(RoutePlanningSpecification.hasVehicle(vehiclePublicId))
                .and(RoutePlanningSpecification.hasDeliveryAgent(deliveryAgentPublicId))
                .and(RoutePlanningSpecification.hasRouteWrapper(routeWrapperPublicId))
                .and(RoutePlanningSpecification.hasRegion(region))
                .and(RoutePlanningSpecification.hasDateBetween(from, to))
                .and(RoutePlanningSpecification.hasStatus(status))
                .and(RoutePlanningSpecification.hasDate(date))
                .and(RoutePlanningSpecification.hasWarehouseName(searchParam)), pageable);
    }

    @Override
    public void deleteRoutePlanning(UUID routePlanningPublicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.getRoutePlanningByPublicIdAndDeletedIsFalse(
                routePlanningPublicId);

        if (optionalRoutePlanning.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route Planning not found")
                    .userMessage("Route Planning not found");
        }

        RoutePlanning routePlanning = optionalRoutePlanning.get();
        optionalPerson.ifPresent(routePlanning::setUpdatedBy);
        routePlanning.setDeleted(true);
        routePlanning.setUpdateDate(LocalDateTime.now());

        logisticsHelper.updateVehicleAvailability(UUID.fromString(routePlanning.getVehicleId()), true);
        logisticsHelper.updateAgentAvailability(UUID.fromString(routePlanning.getDeliveryAgentId()), true);

        routePlanning.getSalesOrders().forEach(order -> {
            order.setDeliveryAgent(null);
            order.setRoutePublicId(null);
            order.setSalesOrderStatus(LoadingStatusEnum.PLANNING);
            salesOrdersRepository.save(order);
        });

        routePlanningRepository.save(routePlanning);
    }

    @Deprecated
    @Override
    public void addActionToRoutePlanning(UUID routePlanningPublicId, RoutePlanningRequest routePlanningRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Person not found");
        }

        Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.getRoutePlanningByPublicIdAndDeletedIsFalse(
                routePlanningPublicId);

        if (optionalRoutePlanning.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route Planning not found")
                    .userMessage("Route Planning not found");
        }

        Optional<DeliveryAgents> optionalDeliveryAgent = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(routePlanningRequest.getDeliveryAgentPublicId());

        if (optionalDeliveryAgent.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery Agent not found")
                    .userMessage("Delivery Agent not found");
        }
        DeliveryAgents deliveryAgent = optionalDeliveryAgent.get();

        RoutePlanning routePlanning = optionalRoutePlanning.get();

        if (routePlanningRequest.getAction() == ActionType.ADD) {
            if (routePlanningRequest.getSalesOrdersPublicIds().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales Orders are required")
                        .userMessage("Sales Orders are required");
            }

            // status ctreated only

            if (routePlanning.getRouteStatus() != RouteStatus.CREATED) {
                throw new BadRequestRestApiException()
                        .developerMessage("Route Planning status is not created")
                        .userMessage("Route Planning status is not created");
            }

            List<SalesOrders> salesOrders = routePlanningRequest.getSalesOrdersPublicIds().stream()
                    .map(salesOrderPublicId -> {
                        Optional<SalesOrders> optionalSalesOrders = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrderPublicId);
                        if (optionalSalesOrders.isEmpty()) {
                            throw new BadRequestRestApiException()
                                    .developerMessage("Sales Order not found")
                                    .userMessage("Sales Order not found");
                        }
                        return optionalSalesOrders.get();
                    }).toList();

            BigDecimal totalWeight = salesOrders.stream()
                    .map(SalesOrders::getTotalSalesOrderWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal currentWeight = routePlanning.getSalesOrders().stream()
                    .map(SalesOrders::getTotalSalesOrderWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalWeight.add(currentWeight).compareTo(new BigDecimal(routePlanning.getVehicleWeight())) > 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Total weight is greater than vehicle weight")
                        .userMessage("Total weight is greater than vehicle weight");
            }

            salesOrders.forEach(salesOrders1 -> {
                salesOrders1.setDeliveryAgent(deliveryAgent);
                salesOrders1.setSalesOrderStatus(LoadingStatusEnum.PENDING_LOADING);
                salesOrders1.setRoutePublicId(routePlanning.getPublicId().toString());
                routePlanning.addSalesOrder(salesOrders1);
                salesOrdersRepository.save(salesOrders1);
            });
        } else if (routePlanningRequest.getAction() == ActionType.REMOVE) {

            if (routePlanning.getRouteStatus() != RouteStatus.CREATED) {
                throw new BadRequestRestApiException()
                        .developerMessage("Route Planning status is not created")
                        .userMessage("Route Planning status is not created");
            }

            if (routePlanningRequest.getSalesOrdersPublicIds().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales Orders are required")
                        .userMessage("Sales Orders are required");
            }

            List<SalesOrders> salesOrders = routePlanningRequest.getSalesOrdersPublicIds().stream()
                    .map(salesOrderPublicId -> {
                        Optional<SalesOrders> optionalSalesOrders = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrderPublicId);
                        if (optionalSalesOrders.isEmpty()) {
                            throw new BadRequestRestApiException()
                                    .developerMessage("Sales Order not found")
                                    .userMessage("Sales Order not found");
                        }
                        return optionalSalesOrders.get();
                    }).toList();

            salesOrders.forEach(salesOrders1 -> {
                if (salesOrders1.getRoutePublicId() != null && !salesOrders1.getRoutePublicId().isEmpty() && !salesOrders1
                        .getRoutePublicId()
                        .equals(routePlanning.getPublicId().toString())) {
                    salesOrders1.setDeliveryAgent(null);
                    salesOrders1.setSalesOrderStatus(LoadingStatusEnum.PLANNING);
                    salesOrders1.setRoutePublicId(null);
                    routePlanning.removeSalesOrder(salesOrders1);
                    salesOrdersRepository.save(salesOrders1);
                }
            });
        } else if (routePlanningRequest.getAction() == ActionType.SUBMI) {
            routePlanning.setRouteStatus(RouteStatus.SUBMITTED);
        } else if (routePlanningRequest.getAction() == ActionType.TRANSFER) {
            if (Validators.anyEqualNull(routePlanningRequest.getVehiclePublicId(), routePlanningRequest.getDeliveryAgentPublicId())) {
                throw new BadRequestRestApiException()
                        .developerMessage("Vehicle and Delivery Agent are required")
                        .userMessage("Vehicle and Delivery Agent are required");
            }

            if (routePlanning.getRouteStatus() == RouteStatus.TRANSFERRED) {
                throw new BadRequestRestApiException()
                        .developerMessage("Route has already been transferred")
                        .userMessage("Route has already been transferred");
            }

            if (routePlanning.getRouteStatus() == RouteStatus.DELIVERED || routePlanning.getRouteStatus() == RouteStatus.TRIP_COMPLETED) {
                throw new BadRequestRestApiException()
                        .developerMessage("Cannot transfer a delivered or completed route")
                        .userMessage("Cannot transfer a delivered or completed route");
            }

            Optional<Vehicle> optionalVehicle = vehicleRepository.findByPublicIdAndDeletedIsFalse(routePlanningRequest.getVehiclePublicId());

            if (optionalVehicle.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Vehicle not found")
                        .userMessage("Vehicle not found");
            }

            Vehicle vehicle = optionalVehicle.get();

            // check weight

            BigDecimal totalWeight = routePlanning.getSalesOrders().stream()
                    .map(SalesOrders::getTotalSalesOrderWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalWeight.compareTo(new BigDecimal(vehicle.getWeight())) > 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Total weight is greater than vehicle weight")
                        .userMessage("Total weight is greater than vehicle weight");
            }

            RoutePlanning newRoutePlanning = new RoutePlanning();

            newRoutePlanning.setVehicleName(vehicle.getNumberPlate());
            newRoutePlanning.setVehicleId(vehicle.getPublicId().toString());
            newRoutePlanning.setVehicleWeight(String.valueOf(vehicle.getWeight()));
            newRoutePlanning.setWarehouseName(routePlanning.getWarehouseName());
            newRoutePlanning.setWarehouseId(routePlanning.getWarehouseId());
            newRoutePlanning.setDeliveryAgentName(deliveryAgent.getFullName());
            newRoutePlanning.setDeliveryAgentId(deliveryAgent.getPublicId().toString());
            newRoutePlanning.setRouteId("RT-" + counterService.getNextCounter(CounterType.ROUTE));
            newRoutePlanning.setRouteRegionId(routePlanning.getRouteRegionId());
            newRoutePlanning.setRouteRegionName(routePlanning.getRouteRegionName());
            newRoutePlanning.setSalesOrders(routePlanning.getSalesOrders());
            optionalPerson.ifPresent(newRoutePlanning::setCreatedBy);
            optionalPerson.ifPresent(newRoutePlanning::setUpdatedBy);
            newRoutePlanning.setCreationTime(LocalDateTime.now());
            newRoutePlanning.setRouteStatus(routePlanning.getRouteStatus());

            routePlanning.setRouteStatus(RouteStatus.TRANSFERRED);
            routePlanning.setSalesOrders(null);

            RoutePlanning saved = routePlanningRepository.save(newRoutePlanning);

            Person person = optionalPerson.get();

            saved.getSalesOrders().forEach(salesOrder -> {
                salesOrder.setDeliveryAgent(deliveryAgent);
                salesOrder.setRoutePublicId(saved.getPublicId().toString());

                salesOrder.getSalesOrderItems().forEach(salesOrderItem -> {
                    stockMovementHelper.recordStockMovement(salesOrderItem.getProduct(), salesOrderItem.getQuantity(), StockMovementTransactionType.TRANSFER_BETWEEN_DA,
                            TransactionBoundType.RECORD, person.getFullName(), salesOrder.getWarehouse());
                });

                salesOrdersRepository.save(salesOrder);
            });

            //free up the vehicle and agent
            logisticsHelper.updateVehicleAvailability(UUID.fromString(routePlanning.getVehicleId()), true);
            logisticsHelper.updateAgentAvailability(UUID.fromString(routePlanning.getDeliveryAgentId()), true);

            if (routePlanning.getRouteStatus() == RouteStatus.Loaded || routePlanning.getRouteStatus() == RouteStatus.IN_TRANSIT) {
                logisticsHelper.updateVehicleAvailability(vehicle.getPublicId(), false);
                logisticsHelper.updateAgentAvailability(deliveryAgent.getPublicId(), false);
            }
        }

        optionalPerson.ifPresent(routePlanning::setUpdatedBy);
        routePlanning.setUpdateDate(LocalDateTime.now());

        routePlanningRepository.save(routePlanning);
    }

    @Override
    public Page<SalesOrders> getRoutePlanningOrders(UUID routePlanningPublicId, String searchParam, Pageable pageable) {

        Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.getRoutePlanningByPublicIdAndDeletedIsFalse(
                routePlanningPublicId);

        if (optionalRoutePlanning.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route Planning not found")
                    .userMessage("Route Planning not found");
        }

        RoutePlanning routePlanning = optionalRoutePlanning.get();

//        return new PageImpl<>(routePlanning.getSalesOrders(), pageable, routePlanning.getSalesOrders().size());
        if (searchParam != null && searchParam.length() > 3) {
            return new PageImpl<>(routePlanning.getSalesOrders().stream()
                    .filter(Objects::nonNull)
                    .filter(order -> order.getSalesOrderNumber().toLowerCase().contains(searchParam.toLowerCase()))
                    .toList(), pageable, routePlanning.getSalesOrders().size());
        }
        return new PageImpl<>(routePlanning.getSalesOrders().stream().filter(Objects::nonNull).toList(), pageable, routePlanning.getSalesOrders().size());
    }

    @Override
    public Page<RoutePlanningOrderSlimDTO> getRoutePlanningOrdersSlim(UUID routePlanningPublicId, Pageable pageable) {

        Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.getRoutePlanningByPublicIdAndDeletedIsFalse(
                routePlanningPublicId);

        if (optionalRoutePlanning.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route Planning not found")
                    .userMessage("Route Planning not found");
        }

        List<RoutePlanningOrderSlimDTO> slimOrders = optionalRoutePlanning.get()
                .getSalesOrders()
                .stream()
                .map(order -> {
                    RoutePlanningOrderSlimDTO dto = new RoutePlanningOrderSlimDTO();
                    dto.setPublicId(order.getPublicId());
                    dto.setSalesOrderNumber(order.getSalesOrderNumber());
                    dto.setSalesOrderStatus(order.getSalesOrderStatus() != null ? order.getSalesOrderStatus().name() : null);
                    dto.setDeliveryStatus(order.getDeliveryStatus() != null ? order.getDeliveryStatus().name() : null);
                    dto.setTotalAmountPayable(order.getTotalAmountPayable());
                    dto.setTotalAmountDelivered(order.getTotalAmountDelivered());
                    dto.setTotalDiscount(order.getTotalDiscount());
                    dto.setCreditApplied(order.isCreditApplied());
                    dto.setCustomerAmountToPay(order.getCustomerAmountToPay());
                    dto.setOrderAmendStatus(order.getOrderAmendStatus() != null ? order.getOrderAmendStatus().name() : null);
                    dto.setTraReceiptGenerated(order.getTraReceiptGenerated());
                    if (order.getCustomer() != null) {
                        Customers customer = order.getCustomer();
                        CustomerSlimDTO customerSlim = new CustomerSlimDTO();
                        customerSlim.setPublicId(customer.getPublicId());
                        customerSlim.setName(customer.getName());
                        customerSlim.setAddress(customer.getAddress());
                        customerSlim.setLandmark(customer.getLandmark());
                        customerSlim.setPhoneNumber(customer.getPhoneNumber());
                        customerSlim.setLatitude(customer.getLatitude());
                        customerSlim.setLongitude(customer.getLongitude());
                        customerSlim.setLocationCoordinatesVerified(customer.getLocationCoordinatesVerified());
                        dto.setCustomer(customerSlim);
                    }

                    return dto;
                })
                .toList();

        return new PageImpl<>(slimOrders, pageable, slimOrders.size());
    }

    @Override
    @Transactional
    public RouteProductsDTO getRoutePlanningOrdersAndVanSalesProducts(UUID routePlanningPublicId, Pageable pageable, Person person) {

        Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.getRoutePlanningByPublicIdAndDeletedIsFalse(
                routePlanningPublicId);

        RouteProductsDTO routeProductsDTO = new RouteProductsDTO();

        if (optionalRoutePlanning.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route Planning not found")
                    .userMessage("Route Planning not found");
        }

        RoutePlanning routePlanning = optionalRoutePlanning.get();

        Optional<DeliveryAgents> optionalDeliveryAgent = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(
                routePlanning.getDeliveryAgentId()));

        if (optionalDeliveryAgent.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery Agent not found")
                    .userMessage("Delivery Agent not found");
        }

        DeliveryAgents deliveryAgent = optionalDeliveryAgent.get();

        List<ProductItemDTO> productItemDTOList = new ArrayList<>();
        List<DeliveryAgentOperation> deliveryAgentOperations = new ArrayList<>();
        UUID warehousePublicId = UUID.fromString(routePlanning.getWarehouseId());

        List<SalesOrderItems> salesOrderItems = new ArrayList<>();

        routePlanning.getSalesOrders().forEach(salesOrder -> {
            salesOrderItems.addAll(salesOrder.getSalesOrderItems());

            Map<UUID, List<SalesOrderItems>> groupedOrderItems = salesOrder.getSalesOrderItems().stream()
                    .collect(Collectors.groupingBy(SalesOrderItems::getProductId));

            groupedOrderItems.forEach((productPublicId, items) -> deliveryAgentOperations.add(buildPreBookedDeliveryAgentOperation(
                    routePlanning,
                    deliveryAgent,
                    warehousePublicId,
                    salesOrder.getSalesOrderNumber(),
                    items,
                    person
            )));
        });

        // group by product item public id

        Map<UUID, List<SalesOrderItems>> groupedSalesOrderItems = salesOrderItems.stream()
                .collect(Collectors.groupingBy(SalesOrderItems::getProductId));
        Map<UUID, Integer> preBookedQuantities = new HashMap<>();
        Map<UUID, Long> vanBookedQuantities = new HashMap<>();
        Map<UUID, ProductItemDTO> productMetadata = new HashMap<>();

        groupedSalesOrderItems.forEach((productPublicId, items) -> {

            BigDecimal totalDiscount = items.stream()
                    .filter(SalesOrderItems::isHasDiscount)
                    .map(item -> item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("Total discount for product {} : {}", productPublicId, totalDiscount);

            ProductItemDTO productItemDTO = new ProductItemDTO();
            productItemDTO.setProductName(items.get(0).getProduct().getProductName());
            productItemDTO.setProductItemPublicId(productPublicId);
            productItemDTO.setQuantity(items.stream().map(SalesOrderItems::getQuantity).reduce(0, Integer::sum));
            productItemDTO.setPrice(items.get(0).getPricePerUnit());
            productItemDTO.setProductCode(items.get(0).getProduct().getSku());
            productItemDTO.setDimension(items.get(0).getDimensions());
            productItemDTO.setDiscount(totalDiscount);
            productItemDTOList.add(productItemDTO);

            preBookedQuantities.put(productPublicId,
                    items.stream().map(SalesOrderItems::getQuantity).reduce(0, Integer::sum));
            productMetadata.put(productPublicId, productItemDTO);
        });

        List<VanSales> vanSalesList = vanSalesRepository.findByDeliveryAgents_PublicIdAndStatusAndDeletedIsFalse(deliveryAgent.getPublicId(),
                VanSaleStatus.Pending);

        if (!vanSalesList.isEmpty()) {
            Map<UUID, List<ProductItem>> groupedVanSaleItems = vanSalesList.stream()
                    .flatMap(vanSale -> vanSale.getProductItems().stream())
                    .collect(Collectors.groupingBy(ProductItem::getProductPublicId));

            groupedVanSaleItems.forEach((productPublicId, items) -> {
                long vanSaleQuantity = items.stream().map(ProductItem::getVanSaleQuantity).reduce(0L, Long::sum);
                vanBookedQuantities.merge(productPublicId, vanSaleQuantity, Long::sum);
//                deliveryAgentOperations.add(buildVanSaleDeliveryAgentOperation(
//                        routePlanning,
//                        deliveryAgent,
//                        warehousePublicId,
//                        items,
//                        person
//                ));

                Optional<ProductItemDTO> optionalProductItemDTO = productItemDTOList.stream()
                        .filter(productItemDTO -> productItemDTO.getProductItemPublicId().equals(productPublicId))
                        .findFirst();

                if (optionalProductItemDTO.isPresent()) {
                    ProductItemDTO productItemDTO = optionalProductItemDTO.get();
                    productItemDTO.setQuantity(productItemDTO.getQuantity() + vanSaleQuantity);
                } else {
                    ProductItemDTO productItemDTO = new ProductItemDTO();
                    productItemDTO.setProductName(items.get(0).getProductName());
                    productItemDTO.setProductItemPublicId(productPublicId);
                    productItemDTO.setQuantity(vanSaleQuantity);
                    productItemDTO.setPrice(items.get(0).getProductSellingPrice());
                    productItemDTO.setProductCode(items.get(0).getProductCode());
                    productItemDTO.setDimension("Van sale dimension ");
                    productItemDTOList.add(productItemDTO);
                    productMetadata.put(productPublicId, productItemDTO);
                }
            });
        }

        timingLogHelper.time(
                String.format("Update issuance table routeId=%s products=%d", routePlanning.getRouteId(), productMetadata.size()),
                OperationTimingEntityType.ISSUANCE,
                routePlanning.getRouteId(),
                routePlanning.getPublicId().toString(),
                person == null ? null : person.getEmail(),
                () -> updateIssuanceTable(routePlanning, deliveryAgent, productMetadata, preBookedQuantities, vanBookedQuantities, person)
        );
        updateDeliveryAgentOperations(routePlanning, deliveryAgentOperations);

        if (!productItemDTOList.isEmpty()) {

            PageImpl<ProductItemDTO> productItemDTOPage = new PageImpl<>(productItemDTOList, pageable, productItemDTOList.size());
            routeProductsDTO.setContent(productItemDTOPage.getContent());
            routeProductsDTO.setTotalElements(productItemDTOPage.getTotalElements());
            routeProductsDTO.setPageSize(productItemDTOPage.getSize());
            routeProductsDTO.setLast(productItemDTOPage.isLast());
            routeProductsDTO.setFirst(productItemDTOPage.isFirst());
            routeProductsDTO.setPageNumber(productItemDTOPage.getNumber());
            routeProductsDTO.setTotalPages(productItemDTOPage.getTotalPages());
        }
        return routeProductsDTO;
    }

    private void updateIssuanceTable(RoutePlanning routePlanning,
                                     DeliveryAgents deliveryAgent,
                                     Map<UUID, ProductItemDTO> productMetadata,
                                     Map<UUID, Integer> preBookedQuantities,
                                     Map<UUID, Long> vanBookedQuantities,
                                     Person person) {
        issuanceRepository.deleteByRouteId(routePlanning.getRouteId());

        if (productMetadata.isEmpty()) {
            return;
        }

        UUID warehousePublicId = UUID.fromString(routePlanning.getWarehouseId());

        List<Issuance> issuances = new ArrayList<>();

        for (Map.Entry<UUID, ProductItemDTO> entry : productMetadata.entrySet()) {
            Issuance issuance = new Issuance();
            issuance.setDeliveryAgentPublicId(deliveryAgent.getPublicId());
            issuance.setRouteId(routePlanning.getRouteId());
            issuance.setProductName(entry.getValue().getProductName());
            issuance.setSku(entry.getValue().getProductCode());
            issuance.setWarehousePublicId(warehousePublicId);
            issuance.setCreatedBy(person);
            issuance.setPreBookedQuantity(preBookedQuantities.getOrDefault(entry.getKey(), 0));
            issuance.setVanBookedQuantity(Math.toIntExact(vanBookedQuantities.getOrDefault(entry.getKey(), 0L)));

            issuances.add(issuance);
        }

        issuanceRepository.saveAll(issuances);

    }

    private void updateDeliveryAgentOperations(RoutePlanning routePlanning, List<DeliveryAgentOperation> deliveryAgentOperations) {
        deliveryAgentOperationRepository.deleteByRouteId(routePlanning.getRouteId());

        if (deliveryAgentOperations.isEmpty()) {
            return;
        }

        deliveryAgentOperationRepository.saveAll(deliveryAgentOperations);
    }

    private DeliveryAgentOperation buildPreBookedDeliveryAgentOperation(RoutePlanning routePlanning,
                                                                        DeliveryAgents deliveryAgent,
                                                                        UUID warehousePublicId,
                                                                        String orderNumber,
                                                                        List<SalesOrderItems> items,
                                                                        Person person) {
        int preBookedQuantity = items.stream().map(SalesOrderItems::getQuantity).reduce(0, Integer::sum);

        DeliveryAgentOperation operation = new DeliveryAgentOperation();
        operation.setDeliveryAgentPublicId(deliveryAgent.getPublicId());
        operation.setRouteId(routePlanning.getRouteId());
        operation.setProductName(items.get(0).getProduct().getProductName());
        operation.setSku(items.get(0).getProduct().getSku());
        operation.setWarehousePublicId(warehousePublicId);
        operation.setOrderNumber(orderNumber);
        operation.setPreBookedQuantity(preBookedQuantity);
        operation.setVanBookedQuantity(0);
        operation.setTotalQuantityPicked(preBookedQuantity);
        operation.setCreatedBy(person);
        return operation;
    }

    private DeliveryAgentOperation buildVanSaleDeliveryAgentOperation(RoutePlanning routePlanning,
                                                                      DeliveryAgents deliveryAgent,
                                                                      UUID warehousePublicId,
                                                                      List<ProductItem> items,
                                                                      Person person) {
        int vanBookedQuantity = Math.toIntExact(items.stream().map(ProductItem::getVanSaleQuantity).reduce(0L, Long::sum));

        DeliveryAgentOperation operation = new DeliveryAgentOperation();
        operation.setDeliveryAgentPublicId(deliveryAgent.getPublicId());
        operation.setRouteId(routePlanning.getRouteId());
        operation.setProductName(items.get(0).getProductName());
        operation.setSku(items.get(0).getProductCode());
        operation.setWarehousePublicId(warehousePublicId);
        operation.setOrderNumber(null);
        operation.setPreBookedQuantity(0);
        operation.setVanBookedQuantity(vanBookedQuantity);
        operation.setTotalQuantityPicked(vanBookedQuantity);
        operation.setCreatedBy(person);
        operation.setVanSale(true);
        return operation;
    }

    @Override
    public LiveStockPageable getLiveStockReport(LocalDate localDate, UUID warehouseId,
                                                String searchParam, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LiveStockEntity> cq = cb.createQuery(LiveStockEntity.class);
        Root<LiveStockEntity> root = cq.from(LiveStockEntity.class);

        List<Predicate> andPredicates = new ArrayList<>();
        andPredicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));

        if (warehouseId != null) {
            andPredicates.add(cb.equal(root.get("warehousePublicId"), warehouseId.toString()));
        }

        if (localDate != null) {
            andPredicates.add(cb.equal(root.get("liveDate"), localDate));
        }

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.trim().toUpperCase() + "%";
            Predicate codeMatch = cb.like(cb.upper(root.get("productCode")), search);
            Predicate nameMatch = cb.like(cb.upper(root.get("productName")), search);
            andPredicates.add(cb.or(codeMatch, nameMatch));
        }

        cq.where(cb.and(andPredicates.toArray(new Predicate[0])));
        cq.orderBy(cb.asc(root.get("productName")));

        TypedQuery<LiveStockEntity> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<LiveStockEntity> liveStockEntities = query.getResultList();
        List<LiveStockValue> liveStockValueList = liveStockEntities.stream()
                .map(this::toValue)
                .toList();

//// Count query for total elements
//        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
//        Root<LiveStockEntity> countRoot = countQuery.from(LiveStockEntity.class);
//        countQuery.select(cb.count(countRoot))
//                .where(cb.and(andPredicates.toArray(new Predicate[0])));
//        Long total = entityManager.createQuery(countQuery).getSingleResult();

        PageImpl<LiveStockValue> liveStockValuePage =
                new PageImpl<>(liveStockValueList, pageable, liveStockValueList.size());

        LiveStockPageable liveStockPageable = new LiveStockPageable();
        liveStockPageable.setContent(liveStockValuePage.getContent());
        liveStockPageable.setTotalElements(liveStockValuePage.getTotalElements());
        liveStockPageable.setPageSize(liveStockValuePage.getSize());
        liveStockPageable.setLast(liveStockValuePage.isLast());
        liveStockPageable.setFirst(liveStockValuePage.isFirst());
        liveStockPageable.setPageNumber(liveStockValuePage.getNumber());
        liveStockPageable.setTotalPages(liveStockValuePage.getTotalPages());

        return liveStockPageable;

    }


//    @Override
//    public LiveStockPageable getLiveStockReport(LocalDate localDate, UUID warehouseId,
//                                                String productName, Pageable pageable) {
//
//        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehouseId)
//                .orElseThrow(() -> new BadRequestRestApiException()
//                        .developerMessage("Warehouse not found")
//                        .userMessage("Warehouse not found"));
//
//        Pageable sortedPageable = PageRequest.of(
//                pageable.getPageNumber(),
//                pageable.getPageSize(),
//                Sort.by(Sort.Direction.ASC, "productName")
//        );
//        List<LiveStockValue> liveStockValueList = getLiveStockValueList(localDate, warehouseId, productName, pageable);
//
//        LiveStockPageable liveStockPageable = new LiveStockPageable();
//        PageImpl<LiveStockValue> liveStockValuePage =
//                new PageImpl<>(liveStockValueList, sortedPageable, liveStockValueList.size());
//
//        liveStockPageable.setContent(liveStockValuePage.getContent());
//        liveStockPageable.setTotalElements(liveStockValuePage.getTotalElements());
//        liveStockPageable.setPageSize(liveStockValuePage.getSize());
//        liveStockPageable.setLast(liveStockValuePage.isLast());
//        liveStockPageable.setFirst(liveStockValuePage.isFirst());
//        liveStockPageable.setPageNumber(liveStockValuePage.getNumber());
//        liveStockPageable.setTotalPages(liveStockValuePage.getTotalPages());
//
//        return liveStockPageable;
//    }


    @Override
    public ByteArrayOutputStream generateStaticReportExport(LocalDate reportDate, UUID warehouseId, String productName, Pageable pageable, StatementExportType statementExportType, String name) {

        List<LiveStockValue> liveStockValueList = getLiveStockValueList(reportDate, warehouseId, productName, pageable);

        return staticReport.createReport(liveStockValueList, name);
    }

    private List<LiveStockValue> getLiveStockValueList(LocalDate localDate, UUID warehouseId,
                                                       String productName, Pageable pageable) {

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehouseId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse not found")
                        .userMessage("Warehouse not found"));

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "productName")
        );

        Page<LiveStockEntity> entityPage;

        if (productName != null && !productName.isBlank()) {
            entityPage = liveStockEntityRepository
                    .findAllByWarehousePublicIdAndLiveDateAndDeletedIsFalseAndProductNameIgnoreCaseOrProductCodeIgnoreCase(
                            warehouse.getPublicId().toString(), localDate, productName, productName, sortedPageable);
        } else {
            entityPage = liveStockEntityRepository
                    .findAllByWarehousePublicIdAndLiveDateAndDeletedIsFalse(
                            warehouse.getPublicId().toString(), localDate, sortedPageable);
        }

        return entityPage.stream()
                .map(this::toValue)
                .collect(Collectors.toList());
    }


    private static final List<RouteStatus> TRANSFERABLE_STATUSES = List.of(
            RouteStatus.Loading,
            RouteStatus.Loaded,
            RouteStatus.ReadyToGo,
            RouteStatus.IN_TRANSIT
    );

    @Override
    @Transactional
    public void transferOrderInTransit(@Valid TransferInTransitRequest request, String name) {

        Person actor = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Person not found: " + name)
                        .userMessage("Person not found"));

        RoutePlanning source = routePlanningRepository.findByPublicIdAndDeletedIsFalse(request.getSourceRoutePublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Source route not found")
                        .userMessage("Source route not found"));

        if (!TRANSFERABLE_STATUSES.contains(source.getRouteStatus())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Transfer only allowed when route is Loading, Loaded, ReadyToGo or IN_TRANSIT. Current status: " + source.getRouteStatus())
                    .userMessage("Route cannot be transferred in its current status (" + source.getRouteStatus() + ")");
        }

        if (source.getRouteStatus() == RouteStatus.TRANSFERRED) {
            throw new BadRequestRestApiException()
                    .developerMessage("Source route has already been transferred")
                    .userMessage("This route has already been transferred");
        }

        DeliveryAgents newAgent = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(request.getTargetDeliveryAgentPublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Target delivery agent not found")
                        .userMessage("Target delivery agent not found"));

        DeliveryAgents sourceAgent = deliveryAgentsRepository
                .findByPublicIdAndDeletedIsFalse(UUID.fromString(source.getDeliveryAgentId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Source delivery agent not found")
                        .userMessage("Source delivery agent not found"));

        if (sourceAgent.getPublicId().equals(newAgent.getPublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Source and target delivery agent must be different")
                    .userMessage("Please select a different delivery agent");
        }
        List<RoutePlanning> activeTargetRoutes = routePlanningRepository
                .findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusesAndDeletedIsFalse(
                        newAgent.getPublicId().toString(), TRANSFERABLE_STATUSES);
        if (activeTargetRoutes.size() > 1) {
            throw new BadRequestRestApiException()
                    .developerMessage("Target delivery agent has multiple active routes")
                    .userMessage("Target delivery agent has multiple active routes; resolve these before transferring");
        }
        RoutePlanning targetRoute = activeTargetRoutes.isEmpty() ? null : activeTargetRoutes.get(0);

        UUID targetVehiclePublicId = targetRoute != null
                ? UUID.fromString(targetRoute.getVehicleId())
                : request.getTargetVehiclePublicId();
        if (targetVehiclePublicId == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Target vehicle public id is required when no active target route is used")
                    .userMessage("Target vehicle public id is required");
        }

        Vehicle newVehicle = vehicleRepository.findByPublicIdAndDeletedIsFalse(targetVehiclePublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Target vehicle not found")
                        .userMessage("Target vehicle not found"));

        int transferredOrderCount = request.getTransferType() == InTransitTransferType.ROUTE
                ? source.getSalesOrders().size()
                : Optional.ofNullable(request.getOrderPublicIds()).map(List::size).orElse(0);

        if (request.getTransferType() == InTransitTransferType.ROUTE) {
            log.info("[TRANSFER-ROUTE] Transferring entire route {} to agent {} and vehicle {}",
                    source.getRouteId(), newAgent.getPublicId(), newVehicle.getPublicId());
            if (targetRoute == null) {
                handleFullRouteTransfer(source, newAgent, newVehicle, actor);
            } else {
                mergeFullRouteIntoExistingRoute(source, targetRoute, newAgent, actor);
            }
        } else {
            if (request.getOrderPublicIds() == null || request.getOrderPublicIds().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("orderPublicIds must not be empty for ORDER transfer type")
                        .userMessage("Please specify the orders to transfer");
            }
            handleIndividualOrderTransfer(source, newAgent, newVehicle, actor, request.getOrderPublicIds(), targetRoute);
        }

        // handle reason

        if (request.getReasonCodePublicId() != null) {
            RejectionsCode rejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(request.getReasonCodePublicId())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Rejection reason not found")
                            .userMessage("Rejection reason not found"));
            if (rejectionsCode.getModule() != Module.TransferRouteOrders) {

                throw new BadRequestRestApiException()
                        .userMessage("Rejection reason is not valid for route transfer")
                        .developerMessage("Rejection reason is not valid for route transfer");
            }
            eventsRepository.save(new Events(
                    source.getPublicId().toString(),
                    "TRANSFER_ROUTE",
                    LocalDateTime.now(),
                    request.getReason() != null ? request.getReason() : rejectionsCode.getDescription(),
                    "TRANSFER",
                    actor
            ));
        }

        sendTransferNotifications(sourceAgent, newAgent, source.getRouteId(), request.getTransferType(), transferredOrderCount);
    }

    @Override
    public Page<Issuance> getIssuance(Pageable pageable) {
        return issuanceRepository.findAll(pageable);
    }

    @Override
    public Page<OperationTimingLog> getOperationTimingLogs(OperationTimingEntityType entityType,
                                                           OperationTimingStatus status,
                                                           String routeId,
                                                           String routePublicId,
                                                           String date,
                                                           Pageable pageable) {
        LocalDateTime startOfDate = null;
        LocalDateTime endOfDate = null;

        if (date != null && !date.isBlank()) {
            LocalDate parsedDate = LocalDate.parse(date);
            startOfDate = parsedDate.atStartOfDay();
            endOfDate = parsedDate.plusDays(1).atStartOfDay();
        }

        return operationTimingLogRepository.searchLogs(entityType, status, routeId, routePublicId, startOfDate, endOfDate, pageable);
    }

    @Override
    public Page<DeliveryAgentOperation> getDeliveryAgentOperation(Pageable pageable) {
        return deliveryAgentOperationRepository.findAll(pageable);
    }

    private void sendTransferNotifications(DeliveryAgents sourceAgent,
                                           DeliveryAgents targetAgent,
                                           String routeId,
                                           InTransitTransferType transferType,
                                           int orderCount) {
        String transferScope = transferType == InTransitTransferType.ROUTE
                ? "route"
                : orderCount + " order(s)";

        itibariSmsService.send(sourceAgent.getPhoneNumber(), String.format(
                "Transfer notice: %s %s transferred to %s.", routeId, transferScope, targetAgent.getFullName()));
        itibariSmsService.send(targetAgent.getPhoneNumber(), String.format(
                "Transfer notice: %s %s assigned to you from %s.", routeId, transferScope, sourceAgent.getFullName()));
    }

    private void handleFullRouteTransfer(RoutePlanning source,
                                         DeliveryAgents newAgent,
                                         Vehicle newVehicle,
                                         Person actor) {

        log.info("[TRANSFER-ROUTE] src={} newAgent={} newVehicle={}",
                source.getRouteId(), newAgent.getPublicId(), newVehicle.getPublicId());

        // Weight check against new vehicle
        BigDecimal totalWeight = source.getSalesOrders().stream()
                .map(SalesOrders::getTotalSalesOrderWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (newVehicle.getWeight() != null
                && totalWeight.compareTo(new BigDecimal(newVehicle.getWeight())) > 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Total route weight exceeds new vehicle capacity")
                    .userMessage("Total route weight exceeds new vehicle capacity");
        }

        RouteStatus sourceStatus = source.getRouteStatus();

        List<SalesOrders> transferredOrders = new ArrayList<>(source.getSalesOrders());
        source.getSalesOrders().clear();
        source.setRouteStatus(RouteStatus.TRANSFERRED);
        source.setUpdatedBy(actor);
        source.setUpdateDate(LocalDateTime.now());
        routePlanningRepository.saveAndFlush(source);

        RoutePlanning newRoute = buildNewRoute(source, newAgent, newVehicle, actor, transferredOrders);
        newRoute.setRouteStatus(sourceStatus);
        RoutePlanning savedNewRoute = routePlanningRepository.saveAndFlush(newRoute);

        // Re-assign sales orders to new route
        savedNewRoute.getSalesOrders().forEach(order -> {
            order.setDeliveryAgent(newAgent);
            order.setRoutePublicId(savedNewRoute.getPublicId().toString());
            // Record stock movement per item
            order.getSalesOrderItems().forEach(item ->
                    stockMovementHelper.recordStockMovement(
                            item.getProduct(),
                            item.getQuantity(),
                            StockMovementTransactionType.TRANSFER_BETWEEN_DA,
                            TransactionBoundType.RECORD,
                            actor.getFullName(),
                            order.getWarehouse()));
            salesOrdersRepository.save(order);
        });

        // A route can be transferred while still Loading, before the normal loading flow
        // has created its reconciliation records. Always leave the new route with both.
        Optional<ProductRecon> sourceRecon = productReconRepository
                .findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(source.getRouteId());
        if (sourceRecon.isPresent()) {
            ProductRecon recon = sourceRecon.get();
            recon.setDeliveryId(savedNewRoute.getRouteId());
            recon.setDeliveryAgent(newAgent.getFullName());
            recon.setDeliveryAgentPublicId(newAgent.getPublicId().toString());
            recon.setVehicleNumber(newVehicle.getNumberPlate());
            recon.setUpdatedBy(actor);
            recon.setUpdateDate(LocalDateTime.now());
            productReconRepository.save(recon);
            ensureTransferredCashReconciliation(recon, savedNewRoute, newAgent, newVehicle, actor);
            recalculateTransferredReconciliation(savedNewRoute, recon, actor);
        } else {
            ProductRecon recon = ensureTransferredProductRecon(source, savedNewRoute, newAgent, newVehicle, actor);
            ensureTransferredCashReconciliation(recon, savedNewRoute, newAgent, newVehicle, actor);
            recalculateTransferredReconciliation(savedNewRoute, recon, actor);
        }
        source.setBalance(BigDecimal.ZERO);
        routePlanningRepository.save(source);
        routePlanningRepository.save(savedNewRoute);

        // Availability: free old DA/vehicle, occupy new DA/vehicle
        logisticsHelper.updateVehicleAvailability(UUID.fromString(source.getVehicleId()), true);
        logisticsHelper.updateAgentAvailability(UUID.fromString(source.getDeliveryAgentId()), true);

        boolean mustOccupyNew = TRANSFERABLE_STATUSES.contains(sourceStatus);
        if (mustOccupyNew) {
            logisticsHelper.updateVehicleAvailability(newVehicle.getPublicId(), false);
            logisticsHelper.updateAgentAvailability(newAgent.getPublicId(), false);
        }

        log.info("[TRANSFER-ROUTE] done. newRouteId={}", savedNewRoute.getRouteId());
    }

    private ProductRecon ensureTransferredProductRecon(RoutePlanning source, RoutePlanning newRoute,
                                                       DeliveryAgents newAgent, Vehicle newVehicle, Person actor) {
        ProductRecon recon = productReconRepository
                .findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(newRoute.getRouteId())
                .orElseGet(ProductRecon::new);
        boolean isNew = recon.getId() == null;

        recon.setDeliveryAgent(newAgent.getFullName());
        recon.setDeliveryAgentPublicId(newAgent.getPublicId().toString());
        recon.setVehicleNumber(newVehicle.getNumberPlate());
        recon.setDeliveryId(newRoute.getRouteId());
        recon.setWarehouseName(source.getWarehouseName());
        recon.setWarehousePublicId(source.getWarehouseId());
        if (isNew) {
            recon.setStatus(ReconStatus.Pending);
            recon.setReconciled(false);
            recon.setCreatedBy(actor);
            recon.setCreationTime(LocalDateTime.now());
        } else {
            recon.setUpdatedBy(actor);
            recon.setUpdateDate(LocalDateTime.now());
        }
        ProductRecon savedRecon = productReconRepository.save(recon);

        if (isNew) {
            Map<UUID, ProductItem> items = new LinkedHashMap<>();
            newRoute.getSalesOrders().forEach(order -> order.getSalesOrderItems().forEach(orderItem -> {
                UUID productId = orderItem.getProduct().getPublicId();
                ProductItem item = items.computeIfAbsent(productId, ignored -> {
                    ProductItem created = new ProductItem();
                    created.setProductRecon(savedRecon);
                    created.setProductReconPublicId(savedRecon.getPublicId().toString());
                    created.setProductPublicId(productId);
                    created.setProductName(orderItem.getProduct().getProductName());
                    created.setProductCode(orderItem.getProduct().getSku());
                    created.setProductSellingPrice(Optional.ofNullable(orderItem.getPricePerUnit()).orElse(BigDecimal.ZERO));
                    created.setLoaded(true);
                    created.setHasOrdered(true);
                    created.setHasVanSale(false);
                    created.setCreationTime(LocalDateTime.now());
                    return created;
                });
                long quantity = orderItem.getQuantity();
                item.setQuantityPicked(item.getQuantityPicked() + quantity);
                item.setTotalUnits(item.getTotalUnits() + quantity);
                item.setOrderQuantity(item.getOrderQuantity() + quantity);
                item.setDiscount(Optional.ofNullable(item.getDiscount()).orElse(BigDecimal.ZERO)
                        .add(Optional.ofNullable(orderItem.getDiscount()).orElse(BigDecimal.ZERO)));
            }));
            savedRecon.setProductItems(items.values().stream().map(productItemRepository::save).collect(Collectors.toList()));
            productReconRepository.save(savedRecon);
        }
        return savedRecon;
    }

    private void ensureTransferredCashReconciliation(ProductRecon productRecon, RoutePlanning newRoute,
                                                     DeliveryAgents newAgent, Vehicle newVehicle, Person actor) {
        Reconciliation reconciliation = reconciliationRepository
                .findByProductReconPublicIdAndDeletedIsFalse(productRecon.getPublicId().toString())
                .orElseGet(Reconciliation::new);
        boolean isNew = reconciliation.getId() == null;
        reconciliation.setDeliveryId(newRoute.getRouteId());
        reconciliation.setRoutePublicId(newRoute.getPublicId().toString());
        reconciliation.setProductReconPublicId(productRecon.getPublicId().toString());
        reconciliation.setDeliveryAgent(newAgent.getFullName());
        reconciliation.setDeliveryAgentPublicId(newAgent.getPublicId().toString());
        reconciliation.setVehicleNumber(newVehicle.getNumberPlate());
        reconciliation.setWarehouseHouseStatus(productRecon.getStatus().name());
        if (isNew) {
            BigDecimal pickedAmount = productItemRepository.findByProductReconAndDeletedIsFalse(productRecon).stream()
                    .map(item -> Optional.ofNullable(item.getProductSellingPrice()).orElse(BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(item.getQuantityPicked())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            reconciliation.setDeliveryAmount(pickedAmount);
            reconciliation.setTotalAmountPicked(pickedAmount);
            reconciliation.setDeliveryStatus("Created");
            reconciliation.setCreatedBy(actor);
            reconciliation.setCreationTime(LocalDateTime.now());
        } else {
            reconciliation.setUpdatedBy(actor);
            reconciliation.setUpdateDate(LocalDateTime.now());
        }
        reconciliationRepository.save(reconciliation);
    }

    private void handleIndividualOrderTransfer(RoutePlanning source,
                                               DeliveryAgents newAgent,
                                               Vehicle newVehicle,
                                               Person actor,
                                               List<UUID> orderPublicIds,
                                               RoutePlanning targetRoute) {

        log.info("[TRANSFER-ORDER] src={} orders={} newAgent={}", source.getRouteId(), orderPublicIds, newAgent.getPublicId());

        // Resolve and validate orders belong to source route
        List<SalesOrders> allSourceOrders = source.getSalesOrders();
        List<SalesOrders> ordersToTransfer = orderPublicIds.stream().distinct().map(orderId -> allSourceOrders.stream()
                .filter(o -> o.getPublicId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Order " + orderId + " is not part of route " + source.getRouteId())
                        .userMessage("Order " + orderId + " does not belong to this route"))).collect(Collectors.toList());

        // Must not transfer all orders (use ROUTE type for that)
        if (ordersToTransfer.size() == allSourceOrders.size()) {
            throw new BadRequestRestApiException()
                    .developerMessage("All orders selected – use ROUTE transfer type instead")
                    .userMessage("All orders selected – please use ROUTE transfer type instead");
        }

        // ---- Check if new DA already has an active route → merge instead of creating ----
        if (targetRoute != null) {
            log.info("[TRANSFER-ORDER] newAgent has active route {} – merging", targetRoute.getRouteId());
            mergeOrdersIntoExistingRoute(source, targetRoute, newAgent, actor, ordersToTransfer);
            return;
        }

        // Weight check for new vehicle
        BigDecimal transferWeight = ordersToTransfer.stream()
                .map(SalesOrders::getTotalSalesOrderWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (newVehicle.getWeight() != null
                && transferWeight.compareTo(new BigDecimal(newVehicle.getWeight())) > 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Transferred order weight exceeds new vehicle capacity")
                    .userMessage("Transferred order weight exceeds new vehicle capacity");
        }

        // Create the destination empty, then use the same persisted stock/order transfer
        // as an existing route. This keeps source and target bookkeeping consistent.
        RoutePlanning newRoute = buildNewRoute(source, newAgent, newVehicle, actor, List.of());
        newRoute.setRouteStatus(source.getRouteStatus());
        RoutePlanning savedNewRoute = routePlanningRepository.save(newRoute);
        ProductRecon newRecon = ensureTransferredProductRecon(source, savedNewRoute, newAgent, newVehicle, actor);
        ensureTransferredCashReconciliation(newRecon, savedNewRoute, newAgent, newVehicle, actor);
        mergeOrdersIntoExistingRoute(source, savedNewRoute, newAgent, actor, ordersToTransfer);

        logisticsHelper.updateVehicleAvailability(newVehicle.getPublicId(), false);
        logisticsHelper.updateAgentAvailability(newAgent.getPublicId(), false);
        log.info("[TRANSFER-ORDER] done. newRouteId={}", savedNewRoute.getRouteId());
    }

    /**
     * Merges {@code ordersToTransfer} from {@code source} route into the {@code targetRoute}
     * that already belongs to {@code newAgent}. Recalculates ProductRecon / ProductItem splits
     * and Reconciliation cash totals for both routes.
     */
    private void mergeOrdersIntoExistingRoute(RoutePlanning source,
                                              RoutePlanning targetRoute,
                                              DeliveryAgents newAgent,
                                              Person actor,
                                              List<SalesOrders> ordersToTransfer) {

        log.info("[TRANSFER-ORDER-MERGE] src={} target={} orderCount={}",
                source.getRouteId(), targetRoute.getRouteId(), ordersToTransfer.size());

        if (source.getPublicId().equals(targetRoute.getPublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Source and target route must be different")
                    .userMessage("Please select a different delivery agent's route");
        }

        validateTargetRouteCapacity(targetRoute, ordersToTransfer);

        // Aggregate transferred quantities before changing either route.
        Map<UUID, Long> transferredQtyByProduct = new HashMap<>();
        Map<UUID, String> productNameByPublicId = new HashMap<>();
        Map<UUID, String> productCodeByPublicId = new HashMap<>();
        Map<UUID, BigDecimal> productPriceByPublicId = new HashMap<>();
        Map<UUID, BigDecimal> productDiscountByPublicId = new HashMap<>();

        ordersToTransfer.forEach(order ->
                order.getSalesOrderItems().forEach(item -> {
                    UUID pid = item.getProduct().getPublicId();
                    transferredQtyByProduct.merge(pid, (long) item.getQuantity(), Long::sum);
                    productNameByPublicId.putIfAbsent(pid, item.getProduct().getProductName());
                    productCodeByPublicId.putIfAbsent(pid, item.getProduct().getSku());
                    productPriceByPublicId.putIfAbsent(pid, item.getPricePerUnit() != null ? item.getPricePerUnit() : BigDecimal.ZERO);
                    productDiscountByPublicId.putIfAbsent(pid, item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO);
                })
        );

        // Resolve both reconciliations and validate source stock before making changes.
        ProductRecon sourceRecon = null;
        ProductRecon targetRecon = null;
        Map<UUID, ProductItem> sourceItems = new HashMap<>();
        if (!transferredQtyByProduct.isEmpty()) {
            sourceRecon = productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(source.getRouteId())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Source product reconciliation not found for route " + source.getRouteId())
                            .userMessage("Source route product reconciliation not found"));
            targetRecon = productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(targetRoute.getRouteId())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Target product reconciliation not found for route " + targetRoute.getRouteId())
                            .userMessage("Target route product reconciliation not found"));
            for (Map.Entry<UUID, Long> entry : transferredQtyByProduct.entrySet()) {
                ProductItem sourceItem = productItemRepository
                        .findByProductReconAndProductPublicIdAndDeletedIsFalse(sourceRecon, entry.getKey())
                        .orElseThrow(() -> new BadRequestRestApiException()
                                .developerMessage("Source reconciliation item not found for product " + entry.getKey())
                                .userMessage("A transferred product is missing from the source reconciliation"));
                if (entry.getValue() <= 0 || sourceItem.getQuantityPicked() < entry.getValue()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Insufficient source reconciliation quantity for product " + entry.getKey())
                            .userMessage("Source reconciliation has insufficient quantity for the transfer");
                }
                sourceItems.put(entry.getKey(), sourceItem);
            }
        }

        if (sourceRecon != null) {
            transferredQtyByProduct.forEach((productPublicId, qtyToMove) -> {
                ProductItem srcItem = sourceItems.get(productPublicId);
                long remaining = srcItem.getQuantityPicked() - qtyToMove;
                srcItem.setQuantityPicked(remaining);
                srcItem.setTotalUnits(remaining);
                srcItem.setOrderQuantity(Math.max(0, srcItem.getOrderQuantity() - qtyToMove));
                srcItem.setHasOrdered(srcItem.getOrderQuantity() > 0);
                if (remaining == 0 && srcItem.getQuantitySold() == 0
                        && srcItem.getQuantityReturned() == 0 && srcItem.getQuantityMissing() == 0
                        && srcItem.getQuantityReceived() == 0) {
                    srcItem.setDeleted(true);
                }
                srcItem.setUpdateDate(LocalDateTime.now());
                productItemRepository.save(srcItem);
            });

            recalculateTransferredReconciliation(source, sourceRecon, actor);
        }

        // Add transferred quantities to the target reconciliation.
        if (targetRecon != null) {
            ProductRecon finalTargetRecon = targetRecon;

            transferredQtyByProduct.forEach((productPublicId, qtyToAdd) -> {
                Optional<ProductItem> existingItemOpt =
                        productItemRepository.findByProductReconAndProductPublicIdAndDeletedIsFalse(finalTargetRecon, productPublicId);

                if (existingItemOpt.isPresent()) {
                    // Product already exists in target recon – increment quantities
                    ProductItem existingItem = existingItemOpt.get();
                    existingItem.setQuantityPicked(existingItem.getQuantityPicked() + qtyToAdd);
                    existingItem.setTotalUnits(existingItem.getQuantityPicked());
                    existingItem.setOrderQuantity(existingItem.getOrderQuantity() + qtyToAdd);
                    existingItem.setHasOrdered(true);
                    existingItem.setLoaded(true);
                    existingItem.setUpdateDate(LocalDateTime.now());
                    productItemRepository.save(existingItem);
                } else {
                    // New product for this recon – create ProductItem
                    ProductItem newItem = new ProductItem();
                    newItem.setProductRecon(finalTargetRecon);
                    newItem.setProductPublicId(productPublicId);
                    newItem.setProductName(productNameByPublicId.get(productPublicId));
                    newItem.setProductCode(productCodeByPublicId.get(productPublicId));
                    newItem.setProductSellingPrice(productPriceByPublicId.getOrDefault(productPublicId, BigDecimal.ZERO));
                    newItem.setDiscount(productDiscountByPublicId.getOrDefault(productPublicId, BigDecimal.ZERO));
                    newItem.setQuantityPicked(qtyToAdd);
                    newItem.setTotalUnits(qtyToAdd);
                    newItem.setLoaded(true);
                    newItem.setHasOrdered(true);
                    newItem.setOrderQuantity(qtyToAdd);
                    newItem.setHasVanSale(false);
                    newItem.setCreationTime(LocalDateTime.now());
                    newItem.setProductReconPublicId(finalTargetRecon.getPublicId().toString());
                    finalTargetRecon.addProductItem(productItemRepository.save(newItem));
                }
            });

            recalculateTransferredReconciliation(targetRoute, targetRecon, actor);
        }

        moveTransferredOrders(source, targetRoute, newAgent, actor, ordersToTransfer);

        log.info("[TRANSFER-ORDER-MERGE] done. targetRouteId={}", targetRoute.getRouteId());
    }


    private void moveTransferredOrders(RoutePlanning source, RoutePlanning targetRoute,
                                       DeliveryAgents newAgent, Person actor, List<SalesOrders> ordersToTransfer) {
        // Release source join-table entries before attaching orders to the destination.
        ordersToTransfer.forEach(source::removeSalesOrder);
        source.setUpdatedBy(actor);
        source.setUpdateDate(LocalDateTime.now());
        if (source.getSalesOrders().isEmpty()) {
            source.setRouteStatus(RouteStatus.TRANSFERRED);
        }
        routePlanningRepository.saveAndFlush(source);

        ordersToTransfer.forEach(order -> {
            targetRoute.addSalesOrder(order);
            order.setDeliveryAgent(newAgent);
            order.setRoutePublicId(targetRoute.getPublicId().toString());
            order.setUpdatedBy(actor);
            order.setUpdateDate(LocalDateTime.now());
            order.getSalesOrderItems().forEach(item ->
                    stockMovementHelper.recordStockMovement(
                            item.getProduct(), item.getQuantity(),
                            StockMovementTransactionType.TRANSFER_BETWEEN_DA,
                            TransactionBoundType.RECORD, actor.getFullName(), order.getWarehouse()));
            salesOrdersRepository.save(order);
        });
        targetRoute.setUpdatedBy(actor);
        targetRoute.setUpdateDate(LocalDateTime.now());
        routePlanningRepository.saveAndFlush(targetRoute);

        // Free the source delivery agent and vehicle if the route became empty.
        if (source.getRouteStatus() == RouteStatus.TRANSFERRED) {
            logisticsHelper.updateVehicleAvailability(UUID.fromString(source.getVehicleId()), true);
            logisticsHelper.updateAgentAvailability(UUID.fromString(source.getDeliveryAgentId()), true);
        }

    }

    private void mergeFullRouteIntoExistingRoute(RoutePlanning source, RoutePlanning targetRoute,
                                                 DeliveryAgents newAgent, Person actor) {
        validateTargetRouteCapacity(targetRoute, source.getSalesOrders());
        ProductRecon sourceRecon = productReconRepository
                .findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(source.getRouteId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .userMessage("Source route product reconciliation not found"));
        ProductRecon targetRecon = productReconRepository
                .findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(targetRoute.getRouteId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .userMessage("Target route product reconciliation not found"));
        List<ProductItem> sourceItems = productItemRepository.findByProductReconAndDeletedIsFalse(sourceRecon);
        for (ProductItem item : sourceItems) {
            Optional<ProductItem> targetItem = productItemRepository
                    .findByProductReconAndProductPublicIdAndDeletedIsFalse(targetRecon, item.getProductPublicId());
            if (targetItem.isPresent()) {
                ProductItem existing = targetItem.get();
                existing.setQuantityPicked(existing.getQuantityPicked() + item.getQuantityPicked());
                existing.setTotalUnits(existing.getQuantityPicked());
                existing.setOrderQuantity(existing.getOrderQuantity() + item.getOrderQuantity());
                existing.setVanSaleQuantity(existing.getVanSaleQuantity() + item.getVanSaleQuantity());
                existing.setQuantitySold(existing.getQuantitySold() + item.getQuantitySold());
                existing.setQuantityMissing(existing.getQuantityMissing() + item.getQuantityMissing());
                existing.setQuantityReturned(existing.getQuantityReturned() + item.getQuantityReturned());
                existing.setQuantityReceived(existing.getQuantityReceived() + item.getQuantityReceived());
                existing.setHasOrdered(existing.isHasOrdered() || item.isHasOrdered());
                existing.setHasVanSale(existing.isHasVanSale() || item.isHasVanSale());
                existing.setLoaded(existing.isLoaded() || item.isLoaded());
                existing.setUpdatedBy(actor);
                existing.setUpdateDate(LocalDateTime.now());
                productItemRepository.save(existing);
                item.setDeleted(true);
            } else {
                sourceRecon.getProductItems().remove(item);
                item.setProductRecon(targetRecon);
                item.setProductReconPublicId(targetRecon.getPublicId().toString());
                targetRecon.addProductItem(item);
            }
            item.setUpdatedBy(actor);
            item.setUpdateDate(LocalDateTime.now());
            productItemRepository.save(item);
        }
        Reconciliation sourceCash = reconciliationRepository
                .findByRoutePublicIdAndDeletedIsFalse(source.getPublicId().toString())
                .orElseThrow(() -> new BadRequestRestApiException().userMessage("Source route cash reconciliation not found"));
        Reconciliation targetCash = reconciliationRepository
                .findByRoutePublicIdAndDeletedIsFalse(targetRoute.getPublicId().toString())
                .orElseThrow(() -> new BadRequestRestApiException().userMessage("Target route cash reconciliation not found"));
        transferCashBalances(sourceCash, targetCash);
        for (CreditRecon credit : new ArrayList<>(sourceCash.getCreditRecons())) {
            credit.setReconciliation(targetCash);
            targetCash.addCreditRecon(credit);
        }
        sourceCash.getCreditRecons().clear();
        recalculateTransferredReconciliation(source, sourceRecon, actor);
        recalculateTransferredReconciliation(targetRoute, targetRecon, actor);
        moveTransferredOrders(source, targetRoute, newAgent, actor, new ArrayList<>(source.getSalesOrders()));
    }

    private void transferCashBalances(Reconciliation source, Reconciliation target) {
        target.setDepositAmount(Optional.ofNullable(target.getDepositAmount()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(source.getDepositAmount()).orElse(BigDecimal.ZERO)));
        source.setDepositAmount(BigDecimal.ZERO);
        target.setDebitNotePaidAmount(Optional.ofNullable(target.getDebitNotePaidAmount()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(source.getDebitNotePaidAmount()).orElse(BigDecimal.ZERO)));
        source.setDebitNotePaidAmount(BigDecimal.ZERO);
        target.setCreditAmount(Optional.ofNullable(target.getCreditAmount()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(source.getCreditAmount()).orElse(BigDecimal.ZERO)));
        source.setCreditAmount(BigDecimal.ZERO);
        target.setExpenseAmount(Optional.ofNullable(target.getExpenseAmount()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(source.getExpenseAmount()).orElse(BigDecimal.ZERO)));
        source.setExpenseAmount(BigDecimal.ZERO);
        target.setTotalDiscount(Optional.ofNullable(target.getTotalDiscount()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(source.getTotalDiscount()).orElse(BigDecimal.ZERO)));
        source.setTotalDiscount(BigDecimal.ZERO);
        target.setReconciliationAmount(Optional.ofNullable(target.getReconciliationAmount()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(source.getReconciliationAmount()).orElse(BigDecimal.ZERO)));
        source.setReconciliationAmount(BigDecimal.ZERO);
    }

    private void validateTargetRouteCapacity(RoutePlanning targetRoute, List<SalesOrders> incomingOrders) {
        if (targetRoute.getVehicleWeight() == null) {
            return;
        }
        BigDecimal weight = java.util.stream.Stream.concat(targetRoute.getSalesOrders().stream(), incomingOrders.stream())
                .map(SalesOrders::getTotalSalesOrderWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (weight.compareTo(new BigDecimal(targetRoute.getVehicleWeight())) > 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Total route weight exceeds new vehicle capacity")
                    .userMessage("Total route weight exceeds new vehicle capacity");
        }
    }

    private void recalculateTransferredReconciliation(RoutePlanning route, ProductRecon recon, Person actor) {
        List<ProductItem> items = productItemRepository.findByProductReconAndDeletedIsFalse(recon);
        BigDecimal picked = items.stream().map(ProductItem::getTotalAmountPicked)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sold = items.stream().map(ProductItem::getTotalAmountSold)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal missing = items.stream().map(ProductItem::getTotalAmountMissing)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(route.getPublicId().toString())
                .ifPresent(cash -> {
                    cash.setTotalAmountPicked(picked);
                    cash.setDeliveryAmount("Delivered".equals(cash.getDeliveryStatus()) ? sold : picked);
                    cash.setMissingAmount(missing);
                    cash.setUpdatedBy(actor);
                    cash.setUpdateDate(LocalDateTime.now());
                    reconciliationRepository.save(cash);
                    route.setBalance(cash.getDeliveryAmount()
                            .subtract(Optional.ofNullable(cash.getDepositAmount()).orElse(BigDecimal.ZERO))
                            .subtract(Optional.ofNullable(cash.getCreditAmount()).orElse(BigDecimal.ZERO)));
                });
        recon.setUpdatedBy(actor);
        recon.setUpdateDate(LocalDateTime.now());
        productReconRepository.save(recon);
    }

    private RoutePlanning buildNewRoute(RoutePlanning source,
                                        DeliveryAgents newAgent,
                                        Vehicle newVehicle,
                                        Person actor,
                                        List<SalesOrders> orders) {

        RoutePlanning r = new RoutePlanning();
        r.setVehicleName(newVehicle.getNumberPlate());
        r.setVehicleId(newVehicle.getPublicId().toString());
        r.setVehicleWeight(String.valueOf(newVehicle.getWeight()));
        r.setWarehouseName(source.getWarehouseName());
        r.setWarehouseId(source.getWarehouseId());
        r.setDeliveryAgentName(newAgent.getFullName());
        r.setDeliveryAgentId(newAgent.getPublicId().toString());
        r.setRouteId("RT-" + counterService.getNextCounter(CounterType.ROUTE));
        r.setRouteRegionId(source.getRouteRegionId());
        r.setRouteRegionName(source.getRouteRegionName());
        r.setSalesOrders(new ArrayList<>(orders));
        r.setCreatedBy(actor);
        r.setUpdatedBy(actor);
        r.setCreationTime(LocalDateTime.now());
        r.setUpdateDate(LocalDateTime.now());
        return r;
    }

    private LiveStockValue toValue(LiveStockEntity entity) {
        LiveStockValue value = new LiveStockValue();
        value.setDate(entity.getLiveDate().atStartOfDay());
        value.setProductName(entity.getProductName());
        value.setOpeningStock(entity.getOpeningStock());
        value.setIssuedStock(entity.getIssuedStock() == null ? 0 : entity.getIssuedStock());
        value.setExpectedStockAfterIssuance(entity.getExpectedAfterIssuedStock() == null ? 0 : entity.getExpectedAfterIssuedStock());
        value.setGoodsReceivedNote(entity.getGoodsReceivedStock() == null ? 0 : entity.getGoodsReceivedStock());
        value.setGoodsReturnedNote(entity.getGoodsReturnedStock() == null ? 0 : entity.getGoodsReturnedStock());
        value.setReturnsFromTrade(entity.getReturnedFromTradeStock() == null ? 0 : entity.getReturnedFromTradeStock());
        value.setReplacements(entity.getReplacedStock() == null ? 0 : entity.getReplacedStock());
        value.setReturnedQuarantineStock(entity.getReturnedFromQuarantineStock() == null ? 0 : entity.getReturnedFromQuarantineStock());
        value.setExpectedClosingStock(entity.getExpectedClosingStock() == null ? 0 : entity.getExpectedClosingStock());
        value.setQuarantineStock(entity.getQuarantineStock() == null ? 0 : entity.getQuarantineStock());
        value.setTotalStock(entity.getTotalStock() == null ? 0 : entity.getTotalStock());
        value.setSoldStock(entity.getSoldStock() == null ? 0 : entity.getSoldStock());
        value.setStockAdjustment(entity.getAdjustedStock() == null ? 0 : entity.getAdjustedStock());
        value.setNegativeAdjustedStock(entity.getNegativeAdjustedStock() == null ? 0 : entity.getNegativeAdjustedStock());
        value.setClosingStock(entity.getClosingStock() == null ? 0 : entity.getClosingStock());
        value.setBonusStock(entity.getBonusStock() == null ? 0 : entity.getBonusStock());
        value.setRebateStock(entity.getRebateStock() == null ? 0 : entity.getRebateStock());
        value.setProductCode(entity.getProductCode() == null ? "" : entity.getProductCode());
        value.setIncomingStock(entity.getIncomingStock() == null ? 0 : entity.getIncomingStock());
        value.setCustomerReturnStock(entity.getCustomerReturnedStock() == null ? 0 : entity.getCustomerReturnedStock());
        return value;
    }
}
