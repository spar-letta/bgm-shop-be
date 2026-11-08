package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.dataTypes.Module;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.ProductItemDTO;
import com.app.itibari.models.response.ReconRouteSummary;
import com.app.itibari.models.response.RouteProductsDTO;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.DiscountHelper;
import com.app.itibari.services.helper.LiveStockHelper;
import com.app.itibari.services.helper.LogisticsHelper;
import com.app.itibari.services.helper.StockUpdateHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.helpers.*;
import com.app.itibari.utils.reportHelper.ProductStockImportTemplate;
import com.app.itibari.utils.reportHelper.StagingReport;
import com.app.itibari.utils.specification.ProductReconSpecification;
import com.app.itibari.utils.specification.ProductStockSpecification;
import com.app.itibari.utils.specification.StockMovementSpecification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.source.ByteArrayOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    private final PersonRepository personRepository;

    private final OrganizationRegionsRepository organizationRegionsRepository;

    private final EventsRepository eventsRepository;

    private final DeliveryAgentsRepository deliveryAgentsRepository;

    private final RoutePlanningRepository routePlanningRepository;

    private final SalesOrdersRepository salesOrdersRepository;

    private final LogisticsHelper logisticsHelper;

    private final ReconciliationRepository reconciliationRepository;

    private final ProductReconRepository productReconRepository;

    private final ProductItemRepository productItemRepository;

    private final ProductRepository productRepository;

    private final VanSalesRepository vanSalesRepository;

    private final RejectionCodeRepository rejectionCodeRepository;

    private final LogisticService logisticService;

    private final LiveStockHelper liveStockHelper;

    private final DiscountHelper discountHelper;

    private final StockUpdateHelper stockUpdateHelper;

    private final ReturnItemRepository returnItemRepository;

    private final ObjectMapper objectMapper;

    private final ReturnStagingRepository returnStagingRepository;
    private final ReconciliationStagingRepository reconciliationStagingRepository;
    private final ReconciliationStagingItemRepository reconciliationStagingItemRepository;
    private final StagingHelper stagingHelper;
    private final StagingReport stagingReport;
    private final StockMovementItemRepository stockMovementItemRepository;
    private final CreditLimitItemRepository creditLimitItemRepository;
    private final ProductStockRepository productStockRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryProductsRepository inventoryProductsRepository;
    private final ProductStockHelper productStockHelper;
    private final ProductStockImportTemplate productStockImportTemplate;
    private final CreditReconHelper creditReconHelper;

    private final WebhookClient webhookClient;
    private final CashCreditReconReportLineRepository cashCreditReconReportLineRepository;
    private final CreditReconRepository creditReconRepository;

    @Override
    public Warehouse createWarehouse(WarehouseRequest warehouseRequest, String name) {

        Warehouse warehouse = new Warehouse();

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<Warehouse> warehouseOptional = warehouseRepository.findByWarehouseNameIgnoreCaseAndDeletedIsFalse(warehouseRequest.getWarehouseName());

        if (warehouseOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse with name " + warehouseRequest.getWarehouseName() + " already exists")
                    .userMessage("Sorry. A warehouse with this name already exists");
        }

        if (Validators.anyEqualNull(warehouseRequest.getWarehouseName(), warehouseRequest.getWarehouseType())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse name and type are required")
                    .userMessage("Sorry.Warehouse name and type are required");
        }

        if (warehouseRequest.isCoreWarehouse()) {
            Optional<Warehouse> coreWarehouseOptional = warehouseRepository.findBySuperWarehouseIsTrueAndDeletedIsFalse();
            if (coreWarehouseOptional.isPresent()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Core Warehouse already exists")
                        .userMessage("Sorry.Core Warehouse already exists");
            }

            if (warehouseRequest.getWarehouseType() == WarehouseType.MAIN) {
                warehouse.setSuperWarehouse(true);
            }
        }

        if (warehouseRequest.getWarehouseType().equals(WarehouseType.MAIN)) {

            Optional<OrganizationRegions> organizationRegionOptional = organizationRegionsRepository
                    .findByPublicIdAndDeletedIsFalse(warehouseRequest.getOrganizationRegionId());

            if (organizationRegionOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Organization Region with public Id " + warehouseRequest.getOrganizationRegionId() + " not found")
                        .userMessage("Sorry. Organization Region not found with public Id " + warehouseRequest.getOrganizationRegionId());
            }

            warehouse.setOrganizationRegion(organizationRegionOptional.get());
            warehouse.setWarehouseName(warehouseRequest.getWarehouseName());
            warehouse.setWarehouseCity(warehouseRequest.getWarehouseCity());
            warehouse.setWarehouseState(warehouseRequest.getWarehouseState());
            warehouse.setWarehouseType(warehouseRequest.getWarehouseType());
            warehouse.setLatitude(warehouseRequest.getLatitude());
            warehouse.setLongitude(warehouseRequest.getLongitude());
            warehouse.setWarehousePostalCode(warehouseRequest.getWarehousePostalCode());
            warehouse.setWarehouseStreet(warehouseRequest.getWarehouseStreet());
        } else {
            // mobile

            if (warehouseRequest.getMainWarehouseId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Main Warehouse Id is required")
                        .userMessage("Sorry.Main Warehouse Id is required");
            }

            Optional<Warehouse> mainWarehouseOptional = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehouseRequest.getMainWarehouseId());

            if (mainWarehouseOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Main Warehouse with public Id " + warehouseRequest.getMainWarehouseId() + " not found")
                        .userMessage("Sorry.Main Warehouse not found with public Id " + warehouseRequest.getMainWarehouseId());
            }

            warehouse.setParentWarehouse(mainWarehouseOptional.get());
            warehouse.setWarehouseType(WarehouseType.MOBILE);
            warehouse.setWarehouseName(mainWarehouseOptional.get().getWarehouseName() + " Mobile");
            warehouse.setWarehouseCity(mainWarehouseOptional.get().getWarehouseCity());
            warehouse.setWarehouseState(mainWarehouseOptional.get().getWarehouseState());
            warehouse.setLatitude(mainWarehouseOptional.get().getLatitude());
            warehouse.setLongitude(mainWarehouseOptional.get().getLongitude());
            warehouse.setWarehousePostalCode(mainWarehouseOptional.get().getWarehousePostalCode());
            warehouse.setWarehouseStreet(mainWarehouseOptional.get().getWarehouseStreet());
        }

        personOptional.ifPresent(warehouse::setCreatedBy);
        warehouse.setCreationTime(LocalDateTime.now());

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        eventsRepository.save(new Events(savedWarehouse.getPublicId().toString(),
                "WAREHOUSE CREATED",
                LocalDateTime.now(),
                "Warehouse created",
                "CREATED",
                personOptional.orElse(null)));

        return savedWarehouse;
    }

    @Override
    public void updateWarehouse(UUID warehousePublicId, WarehouseRequest warehouseRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<Warehouse> warehouseOptional = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehousePublicId);

        if (warehouseOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse with id " + warehousePublicId + " not found")
                    .userMessage("Sorry.Warehouse with id " + warehousePublicId + " not found");
        }

        if (warehouseRequest.getWarehouseType().equals(WarehouseType.MAIN)) {
            Warehouse warehouse = warehouseOptional.get();

            Optional<OrganizationRegions> organizationRegionOptional = organizationRegionsRepository
                    .findByPublicIdAndDeletedIsFalse(warehouseRequest.getOrganizationRegionId());

            if (organizationRegionOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Organization Region with public Id " + warehouseRequest.getOrganizationRegionId() + " not found")
                        .userMessage("Sorry. Organization Region not found");
            }

            warehouse.setOrganizationRegion(organizationRegionOptional.get());
            warehouse.setWarehouseName(warehouseRequest.getWarehouseName());
            warehouse.setWarehouseCity(warehouseRequest.getWarehouseCity());
            warehouse.setWarehouseState(warehouseRequest.getWarehouseState());
            warehouse.setWarehouseType(warehouseRequest.getWarehouseType());
            warehouse.setLatitude(warehouseRequest.getLatitude());
            warehouse.setLongitude(warehouseRequest.getLongitude());
            warehouse.setWarehousePostalCode(warehouseRequest.getWarehousePostalCode());
            warehouse.setWarehouseStreet(warehouseRequest.getWarehouseStreet());

            optionalPerson.ifPresent(warehouse::setUpdatedBy);
            warehouse.setUpdateDate(LocalDateTime.now());

            eventsRepository.save(new Events(warehouse.getPublicId().toString(),
                    "WAREHOUSE UPDATED",
                    LocalDateTime.now(),
                    "Warehouse updated",
                    "UPDATED",
                    optionalPerson.orElse(null)));

            warehouseRepository.save(warehouse);
        } else {
            //update name

            Warehouse warehouse = warehouseOptional.get();
            warehouse.setWarehouseName(warehouseRequest.getWarehouseName());
            optionalPerson.ifPresent(warehouse::setUpdatedBy);
            warehouse.setUpdateDate(LocalDateTime.now());

            eventsRepository.save(new Events(warehouse.getPublicId().toString(),
                    "WAREHOUSE UPDATED",
                    LocalDateTime.now(),
                    "Warehouse updated",
                    "UPDATED",
                    optionalPerson.orElse(null)));

            warehouseRepository.save(warehouse);
        }
    }

    @Override
    public Warehouse getWarehouse(UUID warehousePublicId) {

        Optional<Warehouse> warehouseOptional = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehousePublicId);

        if (warehouseOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse with id " + warehousePublicId + " not found")
                    .userMessage("Sorry.Warehouse with id " + warehousePublicId + " not found");
        }

        return warehouseOptional.get();
    }

    @Override
    public Page<Warehouse> getAllWarehouses(String searchParam, WarehouseType warehouseType, Pageable pageable) {

        if (searchParam != null && warehouseType != null) {
            return warehouseRepository.findByWarehouseNameContainingIgnoreCaseAndWarehouseTypeAndDeletedIsFalse(searchParam,
                    warehouseType,
                    pageable);
        } else if (searchParam != null) {
            return warehouseRepository.findByWarehouseNameContainingIgnoreCaseAndDeletedIsFalse(searchParam, pageable);
        } else if (warehouseType != null) {
            return warehouseRepository.findByWarehouseTypeAndDeletedIsFalse(warehouseType, pageable);
        } else {
            return warehouseRepository.findByDeletedIsFalse(pageable);
        }
    }

    @Override
    public void deleteWarehouse(UUID warehousePublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<Warehouse> warehouseOptional = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehousePublicId);

        if (warehouseOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse with id " + warehousePublicId + " not found")
                    .userMessage("Sorry. Warehouse not found");
        }

        Warehouse warehouse = warehouseOptional.get();
        personOptional.ifPresent(warehouse::setUpdatedBy);
        warehouse.setDeleted(true);
        warehouse.setUpdateDate(LocalDateTime.now());
        warehouseRepository.save(warehouse);
    }

    @Override
    public Page<Warehouse> getMobileWarehousePerMain(UUID warehousePublicId, WarehouseType warehouseType, Pageable pageable) {

        Optional<Warehouse> warehouseOptional = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehousePublicId);

        if (warehouseOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse with id " + warehousePublicId + " not found")
                    .userMessage("Sorry.Warehouse with id " + warehousePublicId + " not found");
        }

        if (warehouseType != null) {
            return warehouseRepository.findByParentWarehouseAndWarehouseTypeAndDeletedIsFalse(warehouseOptional.get(),
                    warehouseType,
                    pageable);
        }

        return warehouseRepository.findByParentWarehouseAndDeletedIsFalse(warehouseOptional.get(), pageable);
    }

    @Override
    public void addRemoveMobileWarehouse(UUID warehousePublicId, WarehouseRequest warehouseRequest, ActionType action, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<Warehouse> warehouseOptional = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehousePublicId);

        if (warehouseOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse with id " + warehousePublicId + " not found")
                    .userMessage("Sorry.Warehouse with id " + warehousePublicId + " not found");
        }

        Warehouse warehouse = warehouseOptional.get();

        if (action.equals(ActionType.ADD)) {
            Warehouse mobileWarehouse = new Warehouse();
            mobileWarehouse.setParentWarehouse(warehouse);
            mobileWarehouse.setWarehouseType(WarehouseType.MOBILE);
            mobileWarehouse.setWarehouseName(warehouseRequest.getWarehouseName() + "- Mobile");
            mobileWarehouse.setWarehouseCity(warehouse.getWarehouseCity());
            mobileWarehouse.setWarehouseState(warehouse.getWarehouseState());
            mobileWarehouse.setLatitude(warehouse.getLatitude());
            mobileWarehouse.setLongitude(warehouse.getLongitude());
            mobileWarehouse.setWarehousePostalCode(warehouse.getWarehousePostalCode());
            mobileWarehouse.setWarehouseStreet(warehouse.getWarehouseStreet());

            personOptional.ifPresent(mobileWarehouse::setCreatedBy);
            mobileWarehouse.setCreationTime(LocalDateTime.now());

            eventsRepository.save(new Events(mobileWarehouse.getPublicId().toString(),
                    "MOBILE WAREHOUSE CREATED",
                    LocalDateTime.now(),
                    "Mobile Warehouse created",
                    "CREATED",
                    personOptional.orElse(null)));

            warehouseRepository.save(mobileWarehouse);
        } else {
            // remove
            warehouse.setDeleted(true);
            personOptional.ifPresent(warehouse::setUpdatedBy);
            warehouse.setUpdateDate(LocalDateTime.now());

            warehouseRepository.save(warehouse);
        }
    }

    @Transactional
    @Override
    public void loadWarehouseProducts(UUID agentPublicId, LoadingRequest loadingRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (!optionalPerson.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User not found")
                    .userMessage("Sorry, user not found");
        }

        Person person = optionalPerson.get();
        Optional<DeliveryAgents> optionalDeliveryAgent = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(agentPublicId);

        if (optionalDeliveryAgent.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery Agent with id " + agentPublicId + " not found")
                    .userMessage("Sorry. Selected delivery agent not found");
        }

        if (loadingRequest.getLoadingDTOList().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Loading DTO List is required")
                    .userMessage("Sorry. Loading item(s) is required");
        }

        loadingRequest.getLoadingDTOList().forEach(loadingDTO -> {
            if (loadingDTO.getProductItemPublicId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product Item Public Id is required")
                        .userMessage("Sorry.Product Item Public Id is required");
            }
        });

        DeliveryAgents deliveryAgent = optionalDeliveryAgent.get();

        List<RoutePlanning> routePlannings = routePlanningRepository.findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusesAndDeletedIsFalse(
                deliveryAgent.getPublicId().toString(),
                Collections.singletonList(RouteStatus.CREATED));

        if (routePlannings.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No route found for delivery agent with id " + agentPublicId)
                    .userMessage("Sorry. No route found for selected delivery agent");
        }

        if (routePlannings.size() != 1) {
            throw new BadRequestRestApiException()
                    .developerMessage("Multiple routes found for delivery agent with id " + agentPublicId)
                    .userMessage("Sorry. Multiple routes found for selected delivery agent");
        }

        RoutePlanning routePlanning = routePlannings.get(0);

        RouteProductsDTO productDTO = logisticService.getRoutePlanningOrdersAndVanSalesProducts(routePlanning.getPublicId(),
                PageRequest.of(0,
                        loadingRequest
                                .getLoadingDTOList()
                                .size()), person);

        List<ProductItemDTO> productItems = productDTO.getContent();

        List<ProductItemDTO> loadedProducts = new ArrayList<>();

        loadingRequest.getLoadingDTOList().forEach(loadingDTO -> {
            Optional<ProductItemDTO> optionalProductItemDTO = productItems.stream()
                    .anyMatch(productItemDTO -> productItemDTO.getProductItemPublicId().equals(loadingDTO.getProductItemPublicId()))
                    ? productItems.stream()
                    .filter(productItemDTO -> productItemDTO.getProductItemPublicId().equals(loadingDTO.getProductItemPublicId()))
                    .findFirst() : Optional.empty();

            log.info("Loading DTO : {}", loadingDTO);

            optionalProductItemDTO.ifPresent(loadedProducts::add);
        });

        loadedProducts.forEach(loadedProduct -> liveStockHelper.updateLiveStockValue(UUID.fromString(routePlanning.getWarehouseId()),
                loadedProduct.getProductItemPublicId(),
                (int) loadedProduct.getQuantity(),
                ProductUpdateType.ISSUED_STOCK,
                LocalDate.now()));

        createUpdateStock(UUID.fromString(routePlanning.getWarehouseId()), loadedProducts);

        // find Unloaded products

        List<ProductItemDTO> unloadedProducts = productItems.stream()
                .filter(productItemDTO -> !loadedProducts.contains(productItemDTO))
                .toList();

        routePlanning.setRouteStatus(RouteStatus.Loaded);
        optionalPerson.ifPresent(routePlanning::setUpdatedBy);
        routePlanning.setUpdateDate(LocalDateTime.now());

        routePlanning.getSalesOrders().forEach(salesOrder -> {
            salesOrder.setSalesOrderStatus(LoadingStatusEnum.LOADED);
            salesOrdersRepository.save(salesOrder);
        });

        eventsRepository.save(new Events(routePlanning.getPublicId().toString(),
                "ROUTE UPDATED",
                LocalDateTime.now(),
                "Route updated",
                "UPDATED",
                optionalPerson.orElse(null)));

        RoutePlanning delivery = routePlanningRepository.save(routePlanning);

        logisticsHelper.updateAgentAvailability(agentPublicId, false);
        createProductReconciliation(delivery, loadedProducts, null);

        deliveryAgent.setLoaded(true);
        deliveryAgent.setUpdatedBy(optionalPerson.orElse(null));
        deliveryAgent.setUpdateDate(LocalDateTime.now());
        deliveryAgentsRepository.save(deliveryAgent);
    }

    @Override
    public void createUpdateStock(UUID wareHouseId, List<ProductItemDTO> loadedProducts) {
        for (ProductItemDTO loadedProduct : loadedProducts) {
            Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(wareHouseId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Warehouse not found")
                            .userMessage("The specified warehouse does not exist or has been deleted."));

            Inventory inventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Inventory not found")
                            .userMessage("The inventory for the specified warehouse does not exist or has been deleted."));

            Product product = productRepository.findByPublicIdAndDeletedIsFalse(loadedProduct.getProductItemPublicId())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Product not found")
                            .userMessage("The specified product does not exist or has been deleted."));

            InventoryProducts inventoryProduct = inventoryProductsRepository.findByInventoryAndProductAndDeletedIsFalse(inventory, product)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Inventory Product not found")
                            .userMessage("The specified product is not associated with the inventory of the warehouse."));

            productStockHelper.updateProductStock(product, warehouse, loadedProduct.getQuantity(),
                    StockMovementTransactionType.ISSUANCE);
        }
    }


    @Override
    public Page<ProductRecon> getAllProductRecons(String searchParam,
                                                  String deliveryAgentPublicId,
                                                  String deliveryId,
                                                  String vehicleNumber,
                                                  Boolean reconciled,
                                                  String warehousePublicId,
                                                  String from,
                                                  String to,
                                                  ReconStatus status,
                                                  UUID createdBy,
                                                  Boolean isWalkIn,
                                                  Pageable pageable) {

        Specification<ProductRecon> specification = Specification.where(
                        ProductReconSpecification.hasDeliveryAgent(deliveryAgentPublicId))
                .and(ProductReconSpecification.hasDeliveryId(deliveryId))
                .and(ProductReconSpecification.hasVehicleNumber(vehicleNumber))
                .and(ProductReconSpecification.hasReconciled(reconciled))
                .and(ProductReconSpecification.hasStatus(status))
                .and(ProductReconSpecification.hasCreatedBy(createdBy))
                .and(ProductReconSpecification.hasWarehousePublicId(warehousePublicId))
                .and(ProductReconSpecification.hasDateBetween(from, to))
                .and(ProductReconSpecification.isNotDeleted())
                .and(ProductReconSpecification.isWalkIn(isWalkIn))
                .and(ProductReconSpecification.hasSearchParam(searchParam));
        return productReconRepository.findAll(specification, pageable);
    }

    @Override
    public Page<ProductItem> getAllProductItemsFromRecon(UUID publicId, String searchParam, Pageable pageable) {

        Optional<ProductRecon> productReconOptional = productReconRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (productReconOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Recon with id " + publicId + " not found")
                    .userMessage("Sorry. Product reconciliation record not found");
        }

        ProductRecon productRecon = productReconOptional.get();

        return productItemRepository.findAllByProductReconAndDeletedIsFalse(productRecon, pageable);
    }

    @Transactional
    @Override
    public void reconcileProductRecon(UUID publicId, ProductReconAction productReconAction, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<ProductRecon> productReconOptional = productReconRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (productReconOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Recon with id " + publicId + " not found")
                    .userMessage("Sorry.Product Recon with id " + publicId + " not found");
        }

        ProductRecon productRecon = productReconOptional.get();
        assertProductReconEditable(productRecon);

        RoutePlanning routePlanning = routePlanningRepository.findByRouteIdAndDeletedIsFalse(productRecon.getDeliveryId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Route not found")
                        .userMessage("Sorry.Route not found"));

        if (routePlanning.getRouteStatus() != RouteStatus.DELIVERED) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id " + productRecon.getDeliveryId() + " is not delivered")
                    .userMessage("Sorry.Route is not delivered yet!");
        }


        if (productRecon.isReconciled()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Recon with id " + publicId + " already reconciled")
                    .userMessage("Sorry.Product Recon  already reconciled");
        }

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(productRecon.getWarehousePublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse with id " + productRecon.getWarehousePublicId() + " not found")
                        .userMessage("Sorry.Warehouse not found"));

        Map<Product, Integer> returnedStock = new HashMap<>();

        Set<ProductItem> productItemSet = new HashSet<>();

        productReconAction.getProductItems().forEach(productItem -> {
            Optional<ProductItem> productItemOptional = productItemRepository.findByPublicIdAndDeletedIsFalse(productItem.getProductItemPublicId());
            if (productItemOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product Item with id " + productItem.getProductItemPublicId() + " not found")
                        .userMessage("Sorry. Selected product item not found");
            }
            if (productItem.getQuantityMissing() > 0 && productItem.getMissingRejectionPublicId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Missing Rejection Public Id is required")
                        .userMessage("Sorry.Missing Rejection Public Id is required");
            }

            ProductItem item = productItemOptional.get();
            item.setQuantityReturned(productItem.getQuantityReturned());
            item.setQuantityMissing(productItem.getQuantityMissing());
            optionalPerson.ifPresent(item::setUpdatedBy);
            item.setUpdateDate(LocalDateTime.now());

            if (productItem.getMissingRejectionPublicId() != null) {
                Optional<RejectionsCode> missingRejectionOptional = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(
                        productItem.getMissingRejectionPublicId());
                if (missingRejectionOptional.isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Missing Rejection with id " + productItem.getMissingRejectionPublicId() + " not found")
                            .userMessage("Sorry. Rejection code not found");
                }
                RejectionsCode missingRejection = missingRejectionOptional.get();
                item.setRejectionCode(missingRejection.getCode());
                item.setRejectionReason(missingRejection.getDescription());
            }

            ProductItem savedProductItem = productItemRepository.save(item);


            if (savedProductItem.getQuantityReturned() > 0) {
                productItemSet.add(savedProductItem);
            }

            if (productItem.getQuantityReturned() > 0) {
                if ((savedProductItem.getQuantityReturned() + savedProductItem.getQuantityMissing() + savedProductItem.getQuantitySold()) != savedProductItem.getQuantityPicked()) {
                    throw new BadRequestRestApiException()
                            .developerMessage(
                                    "Product Item quantity returned + quantity missing + quantity sold is not equal to the quantity of the product item")
                            .userMessage(
                                    "Sorry.Product Item quantity returned + quantity missing + quantity sold is not equal to the quantity of the product item picked");
                }

                productRepository.findByPublicIdAndDeletedIsFalse(savedProductItem.getProductPublicId()).ifPresent(product -> {
                    returnedStock.put(product, returnedStock.getOrDefault(product, 0) + (int) savedProductItem.getQuantityReturned());
                });

//                liveStockHelper.updateLiveStockValue(UUID.fromString(productRecon.getWarehousePublicId()),
//                        savedProductItem.getProductPublicId(),
//                        (int) savedProductItem.getQuantityReturned(),
//                        ProductUpdateType.RETURNED_FROM_TRADE_STOCK,
//                        LocalDate.now());
            }
        });

        if (!productItemSet.isEmpty()) {
            createStaging(productRecon, productItemSet);
        }

        productRecon.setStatus(ReconStatus.AwaitingApproval);
        productReconRepository.save(productRecon);
    }

    private void createStaging(ProductRecon productRecon, Set<ProductItem> productItemSet) {
        ReconciliationStaging reconciliationStaging = new ReconciliationStaging();
        reconciliationStaging.setProductRecon(productRecon);
        reconciliationStaging.setStatus(ReconciliationStageStatus.STAGING);
        reconciliationStaging.setDeliveryAgentName(productRecon.getDeliveryAgent());
        ReconciliationStaging savedReconciliationStaging = reconciliationStagingRepository.save(reconciliationStaging);

        productItemSet.forEach(productItem -> {
            ReconciliationStagingItem reconciliationStagingItem = new ReconciliationStagingItem();
            reconciliationStagingItem.setAnticipatedProduct((int) (productItem.getQuantityPicked() - (int) productItem.getQuantitySold()));
            reconciliationStagingItem.setQuantityReturned((int) productItem.getQuantityReturned());
            reconciliationStagingItem.setReason(productItem.getRejectionCode());
            reconciliationStagingItem.setProductPublicId(productItem.getProductPublicId());
            reconciliationStagingItem.setReconciliationStaging(savedReconciliationStaging);
            reconciliationStagingItem.setProductName(productItem.getProductName());
            reconciliationStagingItemRepository.save(reconciliationStagingItem);
        });
    }

    @Override
    public void reconcileProductReconAction(UUID publicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<ProductRecon> productReconOptional = productReconRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (productReconOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Recon with id " + publicId + " not found")
                    .userMessage("Sorry.Product Recon with id " + publicId + " not found");
        }

        ProductRecon productRecon = productReconOptional.get();
        assertProductReconEditable(productRecon);
        Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.findByRouteIdAndDeletedIsFalse(productRecon.getDeliveryId());

        if (optionalRoutePlanning.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route Planning with id " + productRecon.getDeliveryId() + " not found")
                    .userMessage("Sorry.Route Planning not found");
        }

//        if (productRecon.getStatus() == ReconStatus.AwaitingReturnApproval) {
//            throw new BadRequestRestApiException()
//                    .developerMessage("Product Recon with id " + publicId + " is awaiting return approval")
//                    .userMessage("Sorry.Product Recon is awaiting return approval");
//        }

        RoutePlanning routePlanning = optionalRoutePlanning.get();

        if (routePlanning.getRouteStatus() != RouteStatus.DELIVERED) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route Planning with id " + productRecon.getDeliveryId() + " is not delivered")
                    .userMessage("Sorry.Route Planning is not delivered yet");
        }

        if (productRecon.isReconciled()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Recon with id " + publicId + " already reconciled")
                    .userMessage("Sorry.Product Recon already reconciled");
        }

        if (productRecon.getStatus() != ReconStatus.AwaitingApproval) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Recon with id " + publicId + " is not awaiting approval")
                    .userMessage("Sorry.Product Recon is not awaiting approval");
        }

        productRecon.setReconciled(true);
        productRecon.setStatus(ReconStatus.Reconciled);
        optionalPerson.ifPresent(productRecon::setUpdatedBy);
        productRecon.setUpdateDate(LocalDateTime.now());

        ProductRecon saved = productReconRepository.save(productRecon);

        try {
            updateDiscounts(routePlanning);
        } catch (Exception e) {
            log.error("Error updating product recon discount: {}", e.getMessage());
        }

        eventsRepository.save(new Events(saved.getPublicId().toString(),
                "PRODUCT RECONCILIATION UPDATED",
                LocalDateTime.now(),
                "Product Reconciliation updated",
                "UPDATED",
                optionalPerson.orElse(null)));

        this.createReconciliation(saved, routePlanning.getPublicId().toString(), true);

        try {
            creditReconHelper.processCreditForReconciliation(saved);
        } catch (Exception e) {
            log.error("Error processing credit for reconciliation: {}", e.getMessage());
        }
    }

    @Override
    public void createProductReconciliation(RoutePlanning delivery, List<ProductItemDTO> productItemDTOS, UUID productReconPublicId) {

        ProductRecon productRecon = productReconPublicId != null ? productReconRepository.findByPublicIdAndDeletedIsFalse(productReconPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Product Recon with id " + productReconPublicId + " not found")
                        .userMessage("Sorry. Product reconciliation record not found")) : new ProductRecon();
        assertProductReconEditable(productRecon);

        productRecon.setDeliveryAgent(delivery.getDeliveryAgentName());
        productRecon.setDeliveryAgentPublicId(delivery.getDeliveryAgentId());
        productRecon.setDeliveryId(delivery.getRouteId());
        productRecon.setVehicleNumber(delivery.getVehicleName());
        productRecon.setWarehouseName(delivery.getWarehouseName());
        productRecon.setWarehousePublicId(delivery.getWarehouseId());
        productRecon.setProductItems(new ArrayList<>());
        productRecon.setCreatedBy(delivery.getCreatedBy());
        productRecon.setCreationTime(LocalDateTime.now());
        productRecon.setStatus(ReconStatus.Pending);

        ProductRecon savedProductRecon = productReconRepository.save(productRecon);

        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(delivery.getDeliveryAgentId()));

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery Agent with id " + delivery.getDeliveryAgentId() + " not found")
                    .userMessage("Sorry. Delivery agent not found");
        }

        DeliveryAgents deliveryAgent = deliveryAgentOptional.get();

        List<VanSales> vanSalesList = vanSalesRepository.findByDeliveryAgents_PublicIdAndStatusAndDeletedIsFalse(deliveryAgent.getPublicId(),
                VanSaleStatus.Pending);

        List<ProductItem> productItems = new ArrayList<>();

        if (!vanSalesList.isEmpty()) {
            vanSalesList.forEach(vanSales -> {
                vanSales.getProductItems().forEach(productItem -> {
                    Optional<ProductItemDTO> optionalProductItemDTO = productItemDTOS.stream()
                            .filter(item -> item.getProductItemPublicId().equals(productItem.getProductPublicId()))
                            .findFirst();

                    if (optionalProductItemDTO.isPresent()) {
                        ProductItemDTO productItemDTO = optionalProductItemDTO.get();
                        int salesOrderQuantity = Math.toIntExact(productItemDTO.getQuantity() - productItem.getVanSaleQuantity());
                        productItem.setHasVanSale(true);
                        productItem.setLoaded(true);
                        productItem.setVanSaleItem(true);
                        productItem.setHasOrdered(salesOrderQuantity > 0);
                        productItem.setOrderQuantity(salesOrderQuantity);
                        productItem.setProductPublicId(productItemDTO.getProductItemPublicId());
                        productItem.setQuantityPicked(productItemDTO.getQuantity());
                        productItem.setProductName(productItemDTO.getProductName());
                        productItem.setProductCode(productItemDTO.getProductCode());
                        productItem.setProductRecon(savedProductRecon);
                        productItem.setProductSellingPrice(productItemDTO.getPrice());
                        productItem.setDiscount(productItemDTO.getDiscount());
                        productItems.add(productItemRepository.save(productItem));
                    }
                });
            });
        }

        List<ProductItemDTO> salesItemDTOs = new ArrayList<>();

        productItemDTOS.forEach(productItemDTO -> {
            boolean exists = productItems.stream()
                    .anyMatch(item -> item.getProductPublicId().equals(productItemDTO.getProductItemPublicId()));
            if (!exists) {
                salesItemDTOs.add(productItemDTO);
            }
        });

        if (!salesItemDTOs.isEmpty()) {
            salesItemDTOs.forEach(salesItemDTO -> {
                ProductItem productItem = new ProductItem();
                productItem.setProductRecon(savedProductRecon);
                productItem.setProductName(salesItemDTO.getProductName());
                productItem.setProductPublicId(salesItemDTO.getProductItemPublicId());
                productItem.setQuantityPicked(salesItemDTO.getQuantity());
                productItem.setProductCode(salesItemDTO.getProductCode());
                productItem.setProductSellingPrice(salesItemDTO.getPrice());
                productItem.setLoaded(true);
                productItem.setHasVanSale(false);
                productItem.setHasOrdered(true);
                productItem.setOrderQuantity(salesItemDTO.getQuantity());
                productItem.setDiscount(salesItemDTO.getDiscount());
                ProductItem savedProductItem = productItemRepository.save(productItem);
                productItems.add(savedProductItem);
            });
        }

        savedProductRecon.setProductItems(productItems);
        savedProductRecon.setReconciled(false);
        savedProductRecon.setUpdateDate(LocalDateTime.now());
        savedProductRecon.setUpdatedBy(delivery.getUpdatedBy());
        ProductRecon saved = productReconRepository.save(savedProductRecon);

        if (saved.getProductItems().isEmpty()) {
            productReconRepository.delete(saved);
        } else {
            this.createReconciliation(saved, delivery.getPublicId().toString(), false);
        }

        // update van sales status
        if (!vanSalesList.isEmpty()) {
            vanSalesList.forEach(vanSales -> {
                vanSales.setStatus(VanSaleStatus.Loaded);
                vanSales.setUpdateDate(LocalDateTime.now());
                vanSalesRepository.save(vanSales);
            });
        }
        try {
            updateSalesOrderItem(delivery);
        } catch (Exception e) {
            log.error("Error updating sales order items: {}", e.getMessage());
        }
    }

    @Override
    public ReconRouteSummary getAllProductReconSummary(String routeId) {

        RoutePlanning routePlanning = routePlanningRepository.findByRouteIdAndDeletedIsFalse(routeId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("RoutePlanning Not found")
                        .userMessage("Sorry.RoutePlanning Not found"));

        List<SalesOrders> salesOrders = routePlanning.getSalesOrders();
        int salesOrderSize = salesOrders.size();
        long salesOrdersDeliveredSize = salesOrders.stream()
                .filter(item -> item.getSalesOrderStatus() == LoadingStatusEnum.DELIVERED || item.getDeliveryStatus() == DeliveryStatus.PartiallyDelivered)
                .count();

        Reconciliation reconciliation1 = reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(String.valueOf(routePlanning.getPublicId())).orElse(null);

        if (reconciliation1 == null){
            throw new BadRequestRestApiException()
                    .developerMessage("Missing reconciliation")
                    .userMessage("Sorry. missing reconciliation");
        }

        BigDecimal totalDiscount1 = reconciliation1.getTotalDiscount();
//        BigDecimal totalDiscount = salesOrders.stream()
//                .filter(item -> ((item.getSalesOrderStatus() == LoadingStatusEnum.DELIVERED
//                        || item.getDeliveryStatus() == DeliveryStatus.PartiallyDelivered)) && !item.isCreditApplied())
//                .map(SalesOrders::getTotalDiscount)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ProductRecon productRecon = productReconRepository
                .findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(routePlanning.getRouteId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("ProductRecon Not found")
                        .userMessage("Sorry.ProductRecon Not found"));

        BigDecimal creditTotalAmount = salesOrders.stream()
                .filter(item -> ((item.getSalesOrderStatus() == LoadingStatusEnum.DELIVERED
                        || item.getDeliveryStatus() == DeliveryStatus.PartiallyDelivered)) && item.isCreditApplied())
                .map(SalesOrders::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Reconciliation reconciliation = reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(String.valueOf(routePlanning.getPublicId())).orElse(null);

        BigDecimal totalDeposit = reconciliation != null ? reconciliation.getDepositAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;


        BigDecimal valuePicked = productRecon.getProductItems().stream()
                .map(pi -> pi.getProductSellingPrice().multiply(BigDecimal.valueOf(pi.getQuantityPicked())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valueSold = productRecon.getProductItems().stream()
                .map(pi -> pi.getProductSellingPrice().multiply(BigDecimal.valueOf(pi.getQuantitySold())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenses = BigDecimal.ZERO;
        Optional<CashCreditReconReportLine> cashCreditReconReportLineOptional = cashCreditReconReportLineRepository.findByDeliveryAgentPublicIdAndRoutePublicIdAndDeletedIsFalse(productRecon.getDeliveryAgentPublicId(), routePlanning.getPublicId().toString());
        if (cashCreditReconReportLineOptional.isPresent()) {
            expenses = cashCreditReconReportLineOptional.get().getTotalExpenses();
        }

        BigDecimal balanceValue = valueSold.subtract(totalDiscount1).subtract(totalDeposit).subtract(creditTotalAmount).subtract(expenses);

        BigDecimal totalCredit = routePlanning.getSalesOrders().stream()
                .filter(item -> item.getOrderType() == OrderType.CREDIT)
                .map(SalesOrders::getCreditTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ReconRouteSummary.builder()
                .salesOrderSize(salesOrderSize)
                .salesOrdersDeliveredSize(salesOrdersDeliveredSize)
                .totalDiscount(totalDiscount1)
                .totalDeposit(totalDeposit)
                .valuePicked(valuePicked)
                .valueSold(valueSold)
                .totalExpenses(expenses)
                .totalCredit(totalCredit)
                .balanceValue(balanceValue.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private void updateSalesOrderItem(RoutePlanning delivery) {

        List<SalesOrders> salesOrders = delivery.getSalesOrders();

        Optional<ProductRecon> productReconOptional = productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(
                delivery.getRouteId());
        if (productReconOptional.isEmpty()) {
            return;
        }
        ProductRecon recon = productReconOptional.get();
        if (salesOrders.isEmpty()) {
            return;
        }
        salesOrders.forEach(salesOrder -> {
            salesOrder.getSalesOrderItems().forEach(orderItem -> {
                productItemRepository.findByProductReconAndProductPublicIdAndDeletedIsFalse(recon, orderItem.getProduct().getPublicId())
                        .ifPresent(productItem -> {
                            productItem.setSalesOrderItemPublicId(orderItem.getPublicId().toString());
                            productItemRepository.save(productItem);
                        });
            });
            salesOrdersRepository.save(salesOrder);
        });
    }

    @Override
    public void createReconciliation(ProductRecon productRecon, String routePublicId, boolean update) {

        Reconciliation reconciliation = reconciliationRepository
                .findByProductReconPublicIdAndDeletedIsFalse(productRecon.getPublicId().toString())
                .orElse(new Reconciliation());
        assertReconciliationEditable(reconciliation);

        Optional<RoutePlanning> routePlanningOptional = routePlanningRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(routePublicId));
        if (routePlanningOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route Planning with id " + routePublicId + " not found")
                    .userMessage("Sorry.Route Planning not found");
        }

        RoutePlanning routePlanning = routePlanningOptional.get();


        List<ProductItem> productItems = productRecon.getProductItems();

        BigDecimal totalSold = BigDecimal.ZERO;
        BigDecimal missingAmount = BigDecimal.ZERO;
        BigDecimal totalPicked = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal cashDiscountTotal = BigDecimal.ZERO;
        BigDecimal creditDiscountTotal = BigDecimal.ZERO;

        List<SalesOrders> orders = routePlanning.getSalesOrders();

        for (SalesOrders order : orders) {
            BigDecimal orderDiscount = order.getTotalDiscount();
            if (order.isCreditApplied()) {
                creditDiscountTotal = creditDiscountTotal.add(orderDiscount);
                Optional<CreditLimitItem> optionalCreditLimitItem = creditLimitItemRepository.findByOrderNumberAndStatusAndDeletedIsFalse(
                        order.getSalesOrderNumber(),
                        WalletItemStatus.ACTIVE);

                if (optionalCreditLimitItem.isPresent()) {
                    CreditLimitItem creditLimitItem = optionalCreditLimitItem.get();
                    totalCredit = totalCredit.add(creditLimitItem.getAmount());
                    log.info("[DISCOUNT-TRACE] createReconciliation credit order: order={} creditLimitItemAmount(net)={} orderDiscount={} deliveredGross={}",
                            order.getSalesOrderNumber(), creditLimitItem.getAmount(), orderDiscount, order.getTotalAmountDelivered());
                } else {
                    log.warn("[DISCOUNT-TRACE] createReconciliation: credit order={} has NO ACTIVE CreditLimitItem",
                            order.getSalesOrderNumber());
                }
            } else {
                cashDiscountTotal = cashDiscountTotal.add(orderDiscount);
                if (orderDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    log.info("[DISCOUNT-TRACE] createReconciliation cash order: order={} orderDiscount={}",
                            order.getSalesOrderNumber(), orderDiscount);
                }
            }
        }

        for (ProductItem productItem : productItems) {
            if (productItem != null && productItem.getProductSellingPrice() != null) {
                log.info("===================================");
                log.info("Product Item: {}-----{}", productItem.getProductName(), productItem.getProductSellingPrice());
                totalSold = totalSold.add(productItem.getProductSellingPrice().multiply(BigDecimal.valueOf(productItem.getQuantitySold())));
                missingAmount = missingAmount.add(productItem
                        .getProductSellingPrice()
                        .multiply(BigDecimal.valueOf(productItem.getQuantityMissing())));
                totalPicked = totalPicked.add(productItem
                        .getProductSellingPrice()
                        .multiply(BigDecimal.valueOf(productItem.getQuantityPicked())));
            }
        }

        reconciliation.setDeliveryId(productRecon.getDeliveryId());
        reconciliation.setDeliveryAgentPublicId(productRecon.getDeliveryAgentPublicId());
        reconciliation.setDeliveryAgent(productRecon.getDeliveryAgent());
        reconciliation.setVehicleNumber(productRecon.getVehicleNumber());
        reconciliation.setDeliveryStatus(update ? "Delivered" : "Created");
        reconciliation.setWarehouseHouseStatus(productRecon.getStatus().name());
        reconciliation.setDeliveryAmount(!update ? totalPicked : totalSold);
        reconciliation.setCreatedBy(productRecon.getCreatedBy());
        reconciliation.setCreationTime(LocalDateTime.now());
        reconciliation.setRoutePublicId(routePublicId);
        reconciliation.setProductReconPublicId(productRecon.getPublicId().toString());
        reconciliation.setMissingAmount(missingAmount);
        reconciliation.setTotalAmountPicked(totalPicked);
        reconciliation.setCreditAmount(totalCredit);
        if (update && productRecon.getStatus() == ReconStatus.Reconciled) {
            reconciliation.setTotalDiscount(this.getDiscountAmount(routePublicId));
        }

        reconciliationRepository.save(reconciliation);

        log.info("[DISCOUNT-TRACE] createReconciliation summary: route={} deliveryId={} deliveryAmount(gross)={} creditAmount(net)={} totalDiscount(all orders)={} cashDiscount={} creditDiscount={} outstandingComponents=[delivery={}, deposit={}, totalDiscount={}, creditAmount={}, creditDiscount={}]",
                routePublicId, productRecon.getDeliveryId(), reconciliation.getDeliveryAmount(), totalCredit,
                reconciliation.getTotalDiscount(), cashDiscountTotal, creditDiscountTotal,
                reconciliation.getDeliveryAmount(), reconciliation.getDepositAmount(),
                reconciliation.getTotalDiscount(), reconciliation.getCreditAmount(), reconciliation.getCreditDiscount());
    }

    @Override
    public Page<ProductItem> getAllProductItemsFromLoaded(UUID agentPublicId, String searchParam, String
            deliveryId, Pageable pageable) {

        Optional<ProductRecon> productReconOptional = productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(
                agentPublicId.toString(),
                deliveryId);

        if (productReconOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product recon not found")
                    .userMessage("Sorry. Loaded products not found");
        }

        ProductRecon productRecon = productReconOptional.get();

        log.info("Product items from loaded size {}", productRecon.getProductItems().size());

        return productItemRepository.findAllByProductReconAndDeletedIsFalse(productRecon, pageable);
    }

    @Deprecated
    @Override
    public void loadVanSalesProducts(LoadVanSaleRequest loadVanSaleRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with email " + name + " not found")
                    .userMessage("Sorry. User not found");
        }

        if (Validators.anyEqualNull(loadVanSaleRequest.getVanSalePublicId(),
                loadVanSaleRequest.getRoutePublicId(),
                loadVanSaleRequest.getDeliveryAgentPublicId(),
                loadVanSaleRequest.getProductItems())) {
            throw new BadRequestRestApiException()
                    .developerMessage(
                            "Van Sale Public Id, Route Public Id, Delivery Agent Public Id and Product Item Public Ids are required")
                    .userMessage(
                            "Sorry.Van Sale Public Id, Route Public Id, Delivery Agent Public Id and Product Item Public Ids are required");
        }

        if (loadVanSaleRequest.getProductItems().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Item Public Ids are required")
                    .userMessage("Sorry.Product Item Public Ids are required");
        }

        Optional<VanSales> vanSalesOptional = vanSalesRepository.findByPublicIdAndDeletedIsFalse(loadVanSaleRequest.getVanSalePublicId());

        if (vanSalesOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Van Sale with id " + loadVanSaleRequest.getVanSalePublicId() + " not found")
                    .userMessage("Sorry.Van Sale  not found");
        }

        VanSales vanSales = vanSalesOptional.get();

        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(loadVanSaleRequest.getDeliveryAgentPublicId());

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery Agent with id " + loadVanSaleRequest.getDeliveryAgentPublicId() + " not found")
                    .userMessage("Sorry.Delivery Agent with id  not found");
        }

        DeliveryAgents deliveryAgent = deliveryAgentOptional.get();

        Optional<RoutePlanning> routePlanningOptional = routePlanningRepository.findByPublicIdAndDeletedIsFalse(loadVanSaleRequest.getRoutePublicId());

        if (routePlanningOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route Planning with id " + loadVanSaleRequest.getRoutePublicId() + " not found")
                    .userMessage("Sorry.Route Planning with id not found");
        }

        RoutePlanning routePlanning = routePlanningOptional.get();

        Optional<ProductRecon> optionalProductRecon = productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(
                deliveryAgent.getPublicId().toString(),
                routePlanning.getRouteId().toString());

        if (optionalProductRecon.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route planning not loaded")
                    .userMessage("Sorry.Route planning not loaded");
        }

        ProductRecon productRecon = optionalProductRecon.get();

        if (!vanSales.getDeliveryAgents().getPublicId().equals(deliveryAgent.getPublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Van Sale with id " + loadVanSaleRequest.getVanSalePublicId() + " does not belong to Delivery Agent with id " + loadVanSaleRequest.getDeliveryAgentPublicId())
                    .userMessage("Sorry.Van Sale does not belong to Delivery Agent");
        }

        List<ProductItem> productItems = new ArrayList<>();

        Map<Product, Long> returnedStock = new HashMap<>();

        loadVanSaleRequest.getProductItems().forEach(productItemUpdate -> {
            Optional<ProductItem> optionalProductItem = productItemRepository.findByPublicIdAndDeletedIsFalse(productItemUpdate.getProductItemPublicId());

            if (optionalProductItem.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product Item with id " + productItemUpdate.getProductItemPublicId() + " not found")
                        .userMessage("Sorry. Selected product item not found");
            }

            ProductItem productItem = optionalProductItem.get();

            if (productItem.isLoaded()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product Item with id " + productItemUpdate.getProductItemPublicId() + " already loaded")
                        .userMessage("Sorry.Product Item with id " + productItemUpdate.getProductItemPublicId() + " already loaded");
            }

            if (productItemUpdate.getQuantity() > productItem.getVanSaleQuantity()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product Item with id " + productItemUpdate.getProductItemPublicId() + " quantity is greater than available quantity")
                        .userMessage("Sorry. Requested quantity exceeds available stock");
            }

            if (productItemUpdate.getQuantity() <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product Item with id " + productItemUpdate.getProductItemPublicId() + " quantity is less than or equal to zero")
                        .userMessage("Sorry. Product item quantity must be greater than zero");
            }

            if (productItemUpdate.getQuantity() != productItem.getVanSaleQuantity()) {
                //update inventory and product stock

                long quantity = productItem.getVanSaleQuantity() - productItemUpdate.getQuantity();
                Product product = productRepository.findByPublicId(productItem.getProductPublicId());

                returnedStock.put(product, quantity);
            }

            productItem.setQuantityPicked(productItemUpdate.getQuantity());
            productItem.setLoaded(true);
            productItem.setUpdatedBy(optionalPerson.get());
            productItem.setUpdateDate(LocalDateTime.now());

            ProductItem saved = productItemRepository.save(productItem);

            productItems.add(saved);
        });

        routePlanning.setRouteStatus(RouteStatus.UPDATED);
        routePlanning.setVanSaleGenerated(true);
        routePlanning.setUpdatedBy(optionalPerson.get());
        routePlanning.setUpdateDate(LocalDateTime.now());
        routePlanningRepository.save(routePlanning);

        vanSales.setStatus(VanSaleStatus.Loaded);
        vanSales.setUpdatedBy(optionalPerson.get());
        vanSales.setUpdateDate(LocalDateTime.now());
        vanSales.setRoutePublicId(routePlanning.getPublicId().toString());

        vanSalesRepository.save(vanSales);

        eventsRepository.save(new Events(vanSales.getPublicId().toString(),
                "VAN SALE LOADED",
                LocalDateTime.now(),
                "Van Sale loaded",
                "LOADED",
                optionalPerson.orElse(null)));

        eventsRepository.save(new Events(routePlanning.getPublicId().toString(),
                "ROUTE PLANNING UPDATED",
                LocalDateTime.now(),
                "Route Planning updated",
                "UPDATED",
                optionalPerson.orElse(null)));

        if (!productItems.isEmpty()) {
            productItems.forEach(productRecon::addProductItem);
        }
        productRecon.setUpdatedBy(optionalPerson.get());
        productRecon.setUpdateDate(LocalDateTime.now());
        productReconRepository.save(productRecon);

        // update unloaded van sales products items

        List<ProductItem> unloadedProductItems = productRecon.getProductItems().stream()
                .filter(productItem -> !productItem.isLoaded())
                .toList();

        Map<Product, Long> returnedStock2 = new HashMap<>();
        if (!unloadedProductItems.isEmpty()) {
            unloadedProductItems.forEach(productItem -> {
                productItem.setUpdatedBy(optionalPerson.get());
                productItem.setUpdateDate(LocalDateTime.now());
                Product product = productRepository.findByPublicId(productItem.getProductPublicId());

                returnedStock2.put(product, productItem.getVanSaleQuantity());
                productItemRepository.save(productItem);
            });
        }

        stockUpdateHelper.UnBookVanSales(returnedStock, deliveryAgent.getWarehouse());

        stockUpdateHelper.updateBookedVanSales(returnedStock2, deliveryAgent.getWarehouse());
    }

    @Override
    public ProductRecon reLoadProductRecon(DeliveryAgents deliveryAgent, RoutePlanning routePlanning) {

        return null;
    }

    @Override
    public Warehouse createQuarantineWarehouse(UUID warehousePublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<Warehouse> warehouseOptional = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehousePublicId);

        if (warehouseOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse with id " + warehousePublicId + " not found")
                    .userMessage("Sorry. Warehouse not found");
        }

        Warehouse warehouse = warehouseOptional.get();

        if (warehouse.getParentWarehouse() != null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse with id " + warehousePublicId + " is already a quarantine warehouse")
                    .userMessage("Sorry. Warehouse is already a quarantine warehouse");
        }

        Warehouse quarantineWarehouse = new Warehouse();
        quarantineWarehouse.setWarehouseName(warehouse.getWarehouseName() + "-  Quarantine");
        quarantineWarehouse.setParentWarehouse(warehouse);
        quarantineWarehouse.setSuperWarehouse(false);
        quarantineWarehouse.setCreatedBy(personOptional.orElse(null));
        quarantineWarehouse.setCreationTime(LocalDateTime.now());
        quarantineWarehouse.setDeleted(false);
        quarantineWarehouse.setWarehouseCity(warehouse.getWarehouseCity());
        quarantineWarehouse.setOrganizationRegion(warehouse.getOrganizationRegion());
        quarantineWarehouse.setWarehouseState(warehouse.getWarehouseState());
        quarantineWarehouse.setLatitude(warehouse.getLatitude());
        quarantineWarehouse.setLongitude(warehouse.getLongitude());
        quarantineWarehouse.setWarehouseType(WarehouseType.QUARANTINE);
        quarantineWarehouse.setWarehouseCity(warehouse.getWarehouseCity());
        quarantineWarehouse.setWarehousePostalCode(warehouse.getWarehousePostalCode());
        return warehouseRepository.save(quarantineWarehouse);
    }

    @Override
    public void transferProductsToQuarantineWarehouse(UUID warehousePublicId,
                                                      TransferType transferType,
                                                      TransferProductsToQuarantineWarehouseRequest transferProductsToQuarantineWarehouseRequest,
                                                      String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<Warehouse> warehouseOptional = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehousePublicId);

        if (warehouseOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse with id " + warehousePublicId + " not found")
                    .userMessage("Sorry. Warehouse not found");
        }

        Warehouse warehouse = warehouseOptional.get();

        if (transferProductsToQuarantineWarehouseRequest.getReasonPublicId() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Reason Public Id is required")
                    .userMessage("Sorry.Reason Public Id is required");
        }

        Optional<RejectionsCode> rejectionsCodeOptional = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(
                transferProductsToQuarantineWarehouseRequest.getReasonPublicId());
        if (rejectionsCodeOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Rejection Code with id " + transferProductsToQuarantineWarehouseRequest.getReasonPublicId() + " not found")
                    .userMessage("Sorry. Rejection code not found");
        }

        RejectionsCode rejectionsCode = rejectionsCodeOptional.get();

        List<Warehouse> quarabtineList = warehouseRepository
                .findByParentWarehouseAndDeletedIsFalse(warehouse, Pageable.unpaged())
                .getContent();

        if (quarabtineList.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No quarantine warehouse found for warehouse with id " + warehousePublicId)
                    .userMessage("Sorry. No quarantine warehouse found for selected warehouse");
        }

        if (quarabtineList.size() > 1) {
            throw new BadRequestRestApiException()
                    .developerMessage("Multiple quarantine warehouses found for warehouse with id " + warehousePublicId)
                    .userMessage("Sorry. Multiple quarantine warehouses found for selected warehouse");
        }

        Warehouse quarantineWarehouse = quarabtineList.get(0);

        if (transferProductsToQuarantineWarehouseRequest.getProductItems().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Items are required")
                    .userMessage("Sorry.Product Items are required");
        }
        if (transferType == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Transfer Type is required")
                    .userMessage("Sorry.Transfer Type is required");
        }

        Map<Product, Long> productItemMap = new HashMap<>();

        transferProductsToQuarantineWarehouseRequest.getProductItems().forEach(productItem -> {
            Optional<Product> product = productRepository.findByPublicIdAndDeletedIsFalse(productItem.getProductItemPublicId());

            if (product.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product with id " + productItem.getProductItemPublicId() + " not found")
                        .userMessage("Sorry. Product not found");
            }

            Product productToTransfer = product.get();

            productItemMap.put(productToTransfer, productItem.getQuantity());
        });

        stockUpdateHelper.updateQuarantineTransfer(productItemMap, warehouse, quarantineWarehouse, transferType);

        eventsRepository.save(new Events(warehouse.getPublicId().toString(),
                "PRODUCTS TRANSFERRED TO QUARANTINE",
                LocalDateTime.now(),
                "Products transferred to quarantine warehouse with reason " + rejectionsCode.getDescription(),
                "TRANSFERRED",
                personOptional.orElse(null)));
    }

    @Override
    public void updateProductReconSoldProduct(UUID publicId,
                                              UpdateProductReconSoldProductRequest updateProductReconSoldProductRequest,
                                              String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with email " + name + " not found")
                    .userMessage("Sorry. User not found");
        }
        Person person = personOptional.get();

        Optional<ProductRecon> productReconOptional = productReconRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (productReconOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Recon with id " + publicId + " not found")
                    .userMessage("Sorry.Product Recon not found");
        }

        ProductRecon productRecon = productReconOptional.get();
        assertProductReconEditable(productRecon);

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(productRecon.getWarehousePublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse with id " + productRecon.getWarehousePublicId() + " not found")
                        .userMessage("Sorry.Warehouse not found"));

        if (Validators.anyEqualNull(updateProductReconSoldProductRequest.getProductColumn(),
                updateProductReconSoldProductRequest.getProductItemPublicId()
                ,
                updateProductReconSoldProductRequest.getQuantity(),
                updateProductReconSoldProductRequest.getIncrementType())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Column, Product Item Public Id, Quantity and Increment Type are required")
                    .userMessage("Sorry.Product Column, Product Item Public Id, Quantity and Increment Type are required");
        }

        Optional<ProductItem> optionalProductItem = productItemRepository.findByProductReconAndPublicIdAndDeletedIsFalse(productRecon,
                updateProductReconSoldProductRequest.getProductItemPublicId());

        if (optionalProductItem.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Item with id " + updateProductReconSoldProductRequest.getProductItemPublicId() + " not found in Product Recon")
                    .userMessage("Sorry.Product Item not found in Product Recon");
        }

        ProductItem productItem = optionalProductItem.get();

        String description = "";

        Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(productItem.getProductPublicId());
        if (optionalProduct.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product with id " + productItem.getProductPublicId() + " not found")
                    .userMessage("Sorry.Product not found");
        }
        Product product = optionalProduct.get();

        Reconciliation reconciliation = reconciliationRepository
                .findByProductReconPublicIdAndDeletedIsFalse(productRecon.getPublicId().toString())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Reconciliation not found for Product Recon with id " + productRecon.getPublicId())
                        .userMessage("Sorry.Reconciliation not found for Product Recon"));
        assertReconciliationEditable(reconciliation);

        /**
         *  changed this to a new endpoint
         */
        stockUpdateHelper.updateStockTransfer(product, productItem, updateProductReconSoldProductRequest, person, warehouse);

        reconciliation.setUpdatedBy(person);
        reconciliation.setUpdateDate(LocalDateTime.now());
        reconciliationRepository.save(reconciliation);
        eventsRepository.save(new Events(productRecon.getPublicId().toString(),
                "PRODUCT RECON  PRODUCT UPDATED",
                LocalDateTime.now(),
                description,
                "UPDATED",
                person));
    }

    @Override
    public void updateReconDiscount(ProductRecon savedProductRecon, String routePublicId) {

        Optional<Reconciliation> optionalReconciliation = reconciliationRepository.findByProductReconPublicIdAndDeletedIsFalse(
                savedProductRecon.getPublicId().toString());

        if (optionalReconciliation.isPresent()) {
            var recon = optionalReconciliation.get();
            assertReconciliationEditable(recon);

            BigDecimal discount = getDiscountAmount(routePublicId);

            recon.setTotalDiscount(discount);

            reconciliationRepository.save(recon);
        }
    }

    ///  discounts updates
    private void updateDiscounts(RoutePlanning routePlanning) {

        discountHelper.updateCreateDiscounts(routePlanning);
    }

    private BigDecimal getDiscountAmount(String routePublicId) {

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal cashDiscount = BigDecimal.ZERO;
        BigDecimal creditDiscount = BigDecimal.ZERO;
        RoutePlanning routePlanning = routePlanningRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(routePublicId))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Route Planning with id " + routePublicId + " not found")
                        .userMessage("Sorry.Route Planning not found"));

        List<SalesOrders> salesOrdersList = routePlanning.getSalesOrders();

        if (!salesOrdersList.isEmpty()) {

            for (SalesOrders salesOrders : salesOrdersList) {
                BigDecimal orderDiscount = salesOrders.getTotalDiscount();
                discountAmount = discountAmount.add(orderDiscount);
                if (salesOrders.isCreditApplied()) {
                    creditDiscount = creditDiscount.add(orderDiscount);
                } else {
                    cashDiscount = cashDiscount.add(orderDiscount);
                }
                if (orderDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    log.info("[DISCOUNT-TRACE] getDiscountAmount: order={} orderType={} discount={}",
                            salesOrders.getSalesOrderNumber(),
                            salesOrders.isCreditApplied() ? "CREDIT" : "CASH",
                            orderDiscount);
                }
            }
        }

        log.info("[DISCOUNT-TRACE] getDiscountAmount summary: route={} total={} cash={} credit={}",
                routePublicId, discountAmount, cashDiscount, creditDiscount);

        return discountAmount;
    }

    @Override
    public void stageReturns(UUID productReconPublicId, CreateStageRequest stageReturnsRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with email " + name + " not found")
                    .userMessage("Sorry. User not found");
        }

        Optional<ProductRecon> productReconOptional = productReconRepository.findByPublicIdAndDeletedIsFalse(productReconPublicId);
        if (productReconOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Recon with id " + productReconPublicId + " not found")
                    .userMessage("Sorry.Product Recon not found");
        }

        ProductRecon productRecon = productReconOptional.get();
        Person person = personOptional.get();

        if (stageReturnsRequest.getStageReturnsRequest().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Items are required")
                    .userMessage("Sorry.Product Items are required");
        }

        ReturnStaging returnStaging = new ReturnStaging();
        returnStaging.setDeliveryAgents(deliveryAgentsRepository
                .findByPublicIdAndDeletedIsFalse(UUID.fromString(productRecon.getDeliveryAgentPublicId()))
                .orElse(null));
        returnStaging.setDeliveryId(productRecon.getDeliveryId());
        returnStaging.setCreationTime(LocalDateTime.now());
        returnStaging.setCreatedBy(person);
        returnStaging.setWarehouseId(productRecon.getWarehousePublicId());

        ReturnStaging savedReturnStaging = returnStagingRepository.save(returnStaging);

        List<ReturnItem> returnItems = new ArrayList<>();

        stageReturnsRequest.getStageReturnsRequest().forEach(stageRequest -> {
            Optional<ProductItem> optionalProductItem = productItemRepository.findByProductReconAndPublicIdAndDeletedIsFalse(productRecon,
                    stageRequest.getProductItemPublicId());

            if (optionalProductItem.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product Item with id " + stageRequest.getProductItemPublicId() + " not found in Product Recon")
                        .userMessage("Sorry.Product Item not found in Product Recon");
            }

            ProductItem productItem = optionalProductItem.get();

            ReturnItem returnItem = new ReturnItem();
            returnItem.setProductItem(productItem);
            returnItem.setQuantity(stageRequest.getQuantity());
            returnItem.setCreatedBy(person);
            returnItem.setCreationTime(LocalDateTime.now());
            returnItem.setReturnStaging(savedReturnStaging);

            ReturnItem returnItem1 = returnItemRepository.save(returnItem);

            returnItems.add(returnItem1);
        });
        returnStaging.setReturnItems(returnItems);

        ReturnStaging saved = returnStagingRepository.save(returnStaging);

        if (savedReturnStaging.getReturnItems().isEmpty()) {
            returnStagingRepository.delete(savedReturnStaging);
            return;
        }

        String description = "Goods returned staged with " + saved
                .getReturnItems()
                .size() + " items for delivery id " + saved.getDeliveryId();

        eventsRepository.save(new Events(productRecon.getPublicId().toString(),
                "RETURN STAGED",
                LocalDateTime.now(),
                description,
                "STAGED",
                person));

        productRecon.setStatus(ReconStatus.AwaitingApproval);
        productRecon.setUpdatedBy(person);
        productRecon.setUpdateDate(LocalDateTime.now());
        productReconRepository.save(productRecon);
    }

    @Override
    public Page<ReturnStaging> getAllReturnStaging(String searchParam, Pageable pageable) {

        return returnStagingRepository.findAll(pageable);
    }

    @Override
    public void approveStagedReturn(UUID returnStagePublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with email " + name + " not found")
                    .userMessage("Sorry. User not found");
        }

        Person person = personOptional.get();

        Optional<ReturnStaging> optionalReturnStaging = returnStagingRepository.findByPublicIdAndDeletedIsFalse(returnStagePublicId);

        if (optionalReturnStaging.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("staging not found")
                    .userMessage("Sorry! Stage return not found");
        }

        ReturnStaging returnStaging = optionalReturnStaging.get();

        if (returnStaging.isApproved()) {
            throw new BadRequestRestApiException()
                    .developerMessage("staging approved")
                    .userMessage("Sorry! Stage return already approved");
        }

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(returnStaging.getWarehouseId()))
                .orElse(null);

        if (warehouse == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("no warehouse")
                    .userMessage("Sorry. No warehouse");
        }

        Map<Product, Integer> returnMap = new HashMap<>();
        returnStaging.getReturnItems().forEach(returnItem -> {
            Product product = productRepository.findByPublicId(returnItem.getProductItem().getProductPublicId());

            returnMap.put(product, returnItem.getQuantity());
        });

        stockUpdateHelper.updateStockReturned(returnMap, warehouse, InventoryEvent.APPROVED_STAGE.name());

        returnStaging.setApproved(true);
        returnStaging.setUpdatedBy(person);
        returnStaging.setUpdateDate(LocalDateTime.now());

        returnStagingRepository.save(returnStaging);

        String description = "Goods returned approved with " + returnStaging
                .getReturnItems()
                .size() + " items for delivery id " + returnStaging.getDeliveryId();

        eventsRepository.save(new Events(returnStaging.getPublicId().toString(),
                "RETURN APPROVED",
                LocalDateTime.now(),
                description,
                "APPROVED",
                person));

        ProductRecon productRecon = productReconRepository
                .findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(returnStaging.getDeliveryId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Product Recon with delivery id " + returnStaging.getDeliveryId() + " not found")
                        .userMessage("Sorry.Product Recon not found"));

        //productRecon.setStatus(ReconStatus.AwaitingApproval);
        productRecon.setUpdatedBy(person);
        productRecon.setUpdateDate(LocalDateTime.now());
        productReconRepository.save(productRecon);
    }

    @Override
    public Page<ReconciliationStaging> getReconciliationStagingItems(String searchParam, LocalDate startDate,
                                                                     LocalDate endDate, Pageable pageable) {
        if (startDate != null && endDate != null) {
            LocalDateTime from = startDate.atStartOfDay();
            LocalDateTime to = endDate.plusDays(1).atStartOfDay();
            return reconciliationStagingRepository.findByCreationTimeBetween(from, to, pageable);
        }

        if (searchParam != null && !searchParam.isBlank()) {
            return reconciliationStagingRepository.findByDeliveryAgentNameContainingIgnoreCaseAndDeletedFalse(searchParam, pageable);
        }

        return reconciliationStagingRepository.findByDeletedIsFalse(pageable);
    }


    @Override
    @Transactional
    public void stagingActions(UUID publicId, StagingActionRequest request, String email, UUID itemPublicId) {

        Person person = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Person with email " + email + " not found")
                        .userMessage("Sorry. User not found"));

        ReconciliationStaging staging = reconciliationStagingRepository.findByPublicIdAndDeletedFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Reconciliation staging record not found")
                        .userMessage("Sorry. Reconciliation staging record not found"));

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(
                        UUID.fromString(staging.getProductRecon().getWarehousePublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse not found")
                        .userMessage("Sorry. Warehouse not found"));

        if (request.actionType() == StagingActionRequest.ActionType.Approve) {
            stagingHelper.handleApproval(staging, request, warehouse, email, publicId);
        } else if (request.actionType() == StagingActionRequest.ActionType.Edit_Item) {
            stagingHelper.validateEditItemRequest(request, itemPublicId);
            stagingHelper.handleItemEdit(itemPublicId, request, person, staging.getProductRecon());
        }
    }

    @Override
    public Page<ReconciliationStagingItem> getStagingItems(UUID publicId, Pageable pageable) {
        ReconciliationStaging reconciliationStaging = reconciliationStagingRepository.findByPublicIdAndDeletedFalse(publicId).orElse(null);
        if (reconciliationStaging == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Reconciliation staging record not found")
                    .userMessage("Sorry. Reconciliation staging record not found");
        }

        List<ReconciliationStagingItem> reconciliationStagingItemList =
                reconciliationStaging.getReconciliationStagingItemList();
        return new PageImpl<>(reconciliationStagingItemList, pageable, reconciliationStagingItemList.size());
    }

    @Override
    public ByteArrayOutputStream generateStagingReportExport(LocalDate startDate, LocalDate endDate, String
            searchParam, StatementExportType statementExportType, String name, Pageable pageable) {
        List<ReconciliationStaging> reconciliationStagingItems = getReconciliationStagingItems(searchParam, startDate,
                endDate, pageable).getContent();

        return stagingReport.createStagingReportExcel(reconciliationStagingItems, name);
    }

    @Override
    public Page<StockMovementItem> getAllStockMovements(String searchParam, UUID
            warehousePublicId, StockMovementTransactionType
                                                                transactionType, List<StockMovementTransactionType> transactionTypes, LocalDate startDate, LocalDate
                                                                endDate, Pageable pageable) {
        Specification<StockMovementItem> spec = Specification
                .where(StockMovementSpecification.isNotDeleted())
                .and(StockMovementSpecification.belongsToWarehouse(warehousePublicId))
                .and(StockMovementSpecification.hasTransactionType(transactionType))
                .and(StockMovementSpecification.hasTransactionTypes(transactionTypes))
                .and(StockMovementSpecification.withinDateRange(startDate, endDate))
                .and(StockMovementSpecification.searchParamMatches(searchParam));

        return stockMovementItemRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ProductStock> getAllProductStocks(String searchParam, UUID warehousePublicId, LocalDate startDate, LocalDate endDate, ActiveType activeType, Pageable pageable) {
        boolean isActive = activeType == ActiveType.Active;

        Specification<ProductStock> spec = Specification
                .where(ProductStockSpecification.isNotDeleted())
                .and(ProductStockSpecification.belongsToWarehouse(warehousePublicId))
                .and(ProductStockSpecification.withinDateRange(startDate, endDate))
                .and(ProductStockSpecification.isActiveStatus(!isActive))
                .and(ProductStockSpecification.searchParamMatches(searchParam));

        return productStockRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ProductStockRapperResponse> getProductStockRapper(
            String searchParam,
            UUID warehousePublicId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDate nairobiDate = LocalDate.now(nairobiZone);

        if (warehousePublicId != null) {
            Warehouse warehouse = warehouseRepository
                    .findByPublicIdAndDeletedIsFalse(warehousePublicId)
                    .orElse(null);

            UUID warehouseId = warehouse != null ? warehouse.getPublicId() : null;

            return productStockRepository.getProductStockRapper(pageable, warehouseId, nairobiDate);
        } else {
            return productStockRepository.getProductStockRapper(pageable, null, nairobiDate);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void downloadProductStockTemplate(UUID warehousePublicId, HttpServletResponse response, String prefix, Principal principal) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDate nairobiDate = LocalDate.now(nairobiZone);

        List<ProductStock> productStockList = Collections.emptyList();

        if (warehousePublicId != null) {
            Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(warehousePublicId).orElse(null);
            if (warehouse != null) {
                productStockList = productStockRepository.findByWarehouseAndRecordToday(warehouse, nairobiDate);
            } else {
                log.warn("Warehouse not found for publicId: {}", warehousePublicId);
            }
        } else {
            productStockList = productStockRepository.findByRecordToday(nairobiDate);
        }

        if (!productStockList.isEmpty()) {
            log.info("Downloading template with {} records", productStockList.size());
            productStockImportTemplate.downloadTemplate(response, prefix, productStockList, principal);
        } else {
            log.info("No product stock records found for {}", nairobiDate);
        }
    }

    @Override
    public void uploadProductStockExcel(InputStream inputStream, Principal loggedInUserDetails) throws IOException {
        productStockImportTemplate.uploadTemplate(inputStream, loggedInUserDetails);
    }

    @Override
    public void revertReconcileProductRecon(UUID publicId, UUID reasonPublicId, String email) {
        Person person = personRepository.findByEmailAndDeletedIsFalse(email)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Person with email " + email + " not found")
                        .userMessage("Sorry. User not found"));

        RejectionsCode rejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(reasonPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("reason not found")
                        .userMessage("Sorry. Reason not found")
                );

        if (rejectionsCode.getModule() != Module.ReconRevertReason) {
            throw new BadRequestRestApiException()
                    .developerMessage("Unrelated reason")
                    .userMessage("Sorry. Unrelated reason");
        }

        Optional<ProductRecon> productReconOptional = productReconRepository.findByPublicIdAndDeletedIsFalse(publicId);
        if (productReconOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Recon with id " + publicId + " not found")
                    .userMessage("Sorry.Product Recon not found");
        }

        ProductRecon productRecon = productReconOptional.get();
        assertProductReconEditable(productRecon);

        if (productRecon.getStatus() != ReconStatus.Reconciled) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product Recon not in required state")
                    .userMessage("Sorry! Product Recon is already not reconciled for revert");
        }

        Reconciliation reconciliation = reconciliationRepository.findByDeliveryIdIgnoreCase(productRecon.getDeliveryId());

        if (reconciliation != null) {
            assertReconciliationEditable(reconciliation);
            reconciliation.setReconciled(false);
            reconciliationRepository.save(reconciliation);
        }

        productRecon.setStatus(ReconStatus.ReadyForReconciliation);
        productRecon.setReconciled(false);
        productRecon.setUpdatedBy(person);
        productRecon.setUpdateDate(LocalDateTime.now());
        productReconRepository.save(productRecon);

        String description = String.format("Product recon reverted with reason %s", rejectionsCode.getDescription());

        eventsRepository.save(new Events(publicId.toString(), "REVERT", LocalDateTime.now(), description, "REVERT", person));

        // send google chat notification

        String payload = String.format("Product Recon with delivery id %s has been reverted by %s with reason %s  at %s", productRecon.getDeliveryId(), person.getFullName(),
                rejectionsCode.getDescription(), DateFormatter.formatDateToEastAfrica(LocalDateTime.now()));

        String payloadJson = null;
        try {
            payloadJson = objectMapper.writeValueAsString(Collections.singletonMap("text", payload));
            webhookClient.acceptAndSend("recon", payloadJson);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize TRA error webhook payload for product recon revert with id {}: {}", publicId, e.getMessage());
        }


    }

    private void assertProductReconEditable(ProductRecon productRecon) {
        if (productRecon != null && productRecon.isLocked()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product recon is locked by a reconciled cash credit report")
                    .userMessage("This product reconciliation is locked by a reconciled cash and credit report");
        }
    }

    private void assertReconciliationEditable(Reconciliation reconciliation) {
        if (reconciliation != null && reconciliation.isLocked()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Reconciliation is locked by a reconciled cash credit report")
                    .userMessage("This reconciliation is locked by a reconciled cash and credit report");
        }
    }
}
