package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.dataTypes.Module;
import com.app.itibari.models.CustomerReturnItem;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.TRAResponse;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.helpers.*;
import com.app.itibari.utils.reportHelper.OrderAmendmentExport;
import com.app.itibari.utils.specification.DeliveryAgentSpecification;
import com.app.itibari.utils.specification.LocationApprovalSpecification;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.DocumentException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryAgentsServiceImpl implements DeliveryAgentsService {
    private final LocationApprovalRepository locationApprovalRepository;
    private final VanSalesHelper vanSalesHelper;
    private final TraSwitchService traSwitchService;
    private final CustomerReturnRepository customerReturnRepository;
    private final CounterService counterService;
    private final CustomerReturnItemRepository customerReturnItemRepository;
    private final InventoryProductsRepository inventoryProductsRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductStockHelper productStockHelper;
    private final WebhookClient webhookClient;
    private final DepartmentRepository departmentRepository;
    private final AmendmentToExcel amendmentToExcel;
    private final OrderAmendmentExport orderAmendmentExport;
    @PersistenceContext
    private EntityManager entityManager;

    private final InvoiceItemRepository invoiceItemRepository;


    private final DeliveryAgentsRepository deliveryAgentsRepository;

    private final PersonRepository personRepository;

    private final WarehouseRepository warehouseRepository;

    private final PasswordEncoder passwordEncoder;

    private final ItibariSmsService itibariSmsService;

    private final EventsRepository eventsRepository;

    private final SalesOrdersRepository salesOrdersRepository;

    private final OrderAmendmentRepository orderAmendmentRepository;

    private final RoutePlanningRepository routePlanningRepository;

    private final SalesOrderItemsRepository salesOrderItemsRepository;

    private final ProductReconRepository productReconRepository;

    private final ProductItemRepository productItemRepository;

    private final RejectionCodeRepository rejectionCodeRepository;

    private final WarehouseService warehouseService;

    private final LogisticsHelper logisticsHelper;

    private final CustomersService customersService;

    private final SalesOrdersService salesOrdersService;

    private final VanSalesRepository vanSalesRepository;

    private final NotificationHelper notificationHelper;

    private final OrderAmendmentItemRepository orderAmendmentItemRepository;

    private final LiveStockHelper liveStockHelper;

    private final ProductRepository productRepository;

    private final DiscountHelper discountHelper;

    private final CustomersRepository customersRepository;

    private final DiscountItemsRepository discountItemsRepository;

    private final DiscountRepository discountRepository;

    private final RejectionReasonRepository rejectionReasonRepository;

    private final DeliveryAgentOperationRepository deliveryAgentOperationRepository;

    private final StockUpdateHelper stockUpdateHelper;
    private final SalesAgentsRepository salesAgentsRepository;
    private final SalesOrderAgentRepository salesOrderAgentRepository;
    private final SalesAgentDeliveredItemRepository salesAgentDeliveredItemRepository;
    private final StockMovementHelper stockMovementHelper;
    private final ReconciliationRepository reconciliationRepository;
    private final WalletRepository walletRepository;
    private final CreditLimitItemRepository creditLimitItemRepository;
    private final BnplHelper bnplHelper;
    private final TransactionItemRepository transactionItemRepository;
    private final TraUtilHelper traUtilHelper;
    private final TraSubmissionHelper traSubmissionHelper;
    private final CustomerCheckerHelper customerCheckerHelper;

    private final TumaMessage tumaMessage;


    @Override
    public DeliveryAgents createDeliveryAgent(DeliveryAgentsRequest deliveryAgentsRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<DeliveryAgents> optionalDeliveryAgents = deliveryAgentsRepository.findByIdentificationTypeAndIdentificationNumberIgnoreCaseAndDeletedIsFalse(
                deliveryAgentsRequest.getIdentificationType(), deliveryAgentsRequest.getIdentificationNumber());

        Optional<DeliveryAgents> optionalDeliveryAgentsByPhoneNumber = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(
                deliveryAgentsRequest.getPhoneNumber());

        if (deliveryAgentsRequest.getBulkUpload() != null && deliveryAgentsRequest.getBulkUpload()) {
            if (optionalDeliveryAgentsByPhoneNumber.isPresent() || optionalDeliveryAgents.isPresent()) {
                return null;
            }
        }

        if (optionalDeliveryAgentsByPhoneNumber.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with phone number " + deliveryAgentsRequest.getPhoneNumber() + " already exists")
                    .userMessage("Sorry. Delivery agent with phone number " + deliveryAgentsRequest.getPhoneNumber() + " already exists");
        }

        if (optionalDeliveryAgents.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with identification type " + deliveryAgentsRequest.getIdentificationType() + " and identification number " + deliveryAgentsRequest.getIdentificationNumber() + " already exists")
                    .userMessage("Sorry. Delivery agent with identification type " + deliveryAgentsRequest.getIdentificationType() + " and identification number " + deliveryAgentsRequest.getIdentificationNumber() + " already exists");
        }

        Warehouse warehouse = null;
        Optional<Warehouse> warehouseOptional = warehouseRepository.findByPublicIdAndDeletedIsFalse(deliveryAgentsRequest.getWarehouseId());

        if (deliveryAgentsRequest.getBulkUpload() != null && deliveryAgentsRequest.getBulkUpload()) {
            // find first warehouse
            List<Warehouse> warehouses = warehouseRepository.findAllByDeletedIsFalse();
            if (!warehouses.isEmpty()) {
                warehouse = warehouses.get(0);
            }
        } else {
            if (warehouseOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Warehouse with id " + deliveryAgentsRequest.getWarehouseId() + " not found")
                        .userMessage("Warehouse not found");
            }
            warehouse = warehouseOptional.get();
        }

        DeliveryAgents deliveryAgent = new DeliveryAgents();
        deliveryAgent.setFirstName(deliveryAgentsRequest.getFirstName());
        deliveryAgent.setLastName(deliveryAgentsRequest.getLastName());
        deliveryAgent.setPhoneNumber(deliveryAgentsRequest.getPhoneNumber());
        deliveryAgent.setResidentialAddress(deliveryAgentsRequest.getResidentialAddress());
        deliveryAgent.setWarehouse(warehouse);
        personOptional.ifPresent(deliveryAgent::setCreatedBy);
        deliveryAgent.setCreationTime(LocalDateTime.now());
        deliveryAgent.setIdentificationType(deliveryAgentsRequest.getIdentificationType());
        deliveryAgent.setIdentificationNumber(deliveryAgentsRequest.getIdentificationNumber());
        return deliveryAgentsRepository.save(deliveryAgent);
    }

    @Override
    public void updateDeliveryAgent(UUID agentPublicId, DeliveryAgentsRequest deliveryAgentsRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository
                .findByPublicIdAndDeletedIsFalse(agentPublicId);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with id " + agentPublicId + " not found")
                    .userMessage("Sorry. Delivery agent with id " + agentPublicId + " not found");
        }

        Optional<Warehouse> warehouseOptional = warehouseRepository
                .findByPublicIdAndDeletedIsFalse(deliveryAgentsRequest.getWarehouseId());

        if (warehouseOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse with id " + deliveryAgentsRequest.getWarehouseId() + " not found")
                    .userMessage("Warehouse not found");
        }

        DeliveryAgents deliveryAgent = deliveryAgentOptional.get();
        deliveryAgent.setFirstName(deliveryAgentsRequest.getFirstName());
        deliveryAgent.setLastName(deliveryAgentsRequest.getLastName());
        deliveryAgent.setPhoneNumber(deliveryAgentsRequest.getPhoneNumber());
        deliveryAgent.setResidentialAddress(deliveryAgentsRequest.getResidentialAddress());
        deliveryAgent.setWarehouse(warehouseOptional.get());
        personOptional.ifPresent(deliveryAgent::setUpdatedBy);
        deliveryAgent.setUpdateDate(LocalDateTime.now());
        deliveryAgent.setIdentificationType(deliveryAgentsRequest.getIdentificationType());
        deliveryAgent.setIdentificationNumber(deliveryAgentsRequest.getIdentificationNumber());

        deliveryAgentsRepository.save(deliveryAgent);
    }

    @Override
    @Transactional
    public void updateDeliveryAgentAvailability(UUID deliveryAgentPublicId, boolean available, UUID reasonPublicId, String name) {
        DeliveryAgents deliveryAgent = getDeliveryAgent(deliveryAgentPublicId);
        if (reasonPublicId == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent availability reason is required")
                    .userMessage("Please select a delivery agent availability reason");
        }
        RejectionsCode reason = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(reasonPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Delivery agent availability reason " + reasonPublicId + " not found")
                        .userMessage("Delivery agent availability reason not found"));
        if (reason.getModule() != Module.DeliveryAgentAvailability) {
            throw new BadRequestRestApiException()
                    .developerMessage("Reason " + reasonPublicId + " does not belong to DeliveryAgentAvailability")
                    .userMessage("Please select a reason from the DeliveryAgentAvailability module");
        }

        boolean previouslyAvailable = deliveryAgent.isAvailable();
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        LocalDateTime updatedAt = LocalDateTime.now();
        deliveryAgent.setAvailable(available);
        personOptional.ifPresent(deliveryAgent::setUpdatedBy);
        deliveryAgent.setUpdateDate(updatedAt);
        deliveryAgentsRepository.save(deliveryAgent);

        eventsRepository.save(new Events(deliveryAgentPublicId.toString(),
                "Delivery Agent Availability",
                updatedAt,
                String.format("Delivery agent %s availability changed from %s to %s by %s. Reason: %s (%s)",
                        deliveryAgent.getFullName(),
                        previouslyAvailable ? "available" : "unavailable",
                        available ? "available" : "unavailable", name, reason.getDescription(), reasonPublicId),
                available ? "MARK_AVAILABLE" : "MARK_UNAVAILABLE",
                personOptional.orElse(null)));
    }

    @Override
    public DeliveryAgents getDeliveryAgent(UUID agentPublicId) {

        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository
                .findByPublicIdAndDeletedIsFalse(agentPublicId);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with id " + agentPublicId + " not found")
                    .userMessage("Sorry. Delivery agent with id " + agentPublicId + " not found");
        }

        return deliveryAgentOptional.get();
    }

    @Override
    public Page<DeliveryAgents> getAllDeliveryAgents(String searchParam, Pageable pageable) {

        Specification<DeliveryAgents> specification = Specification
                .where(DeliveryAgentSpecification.searchParamMatches(searchParam))
                .and(DeliveryAgentSpecification.isNotDeleted());

        return deliveryAgentsRepository.findAll(specification, pageable);

    }

    @Override
    public void deleteDeliveryAgent(UUID agentPublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository
                .findByPublicIdAndDeletedIsFalse(agentPublicId);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with id " + agentPublicId + " not found")
                    .userMessage("Sorry. Delivery agent with id " + agentPublicId + " not found");
        }

        DeliveryAgents deliveryAgent = deliveryAgentOptional.get();
        personOptional.ifPresent(deliveryAgent::setUpdatedBy);
        deliveryAgent.setDeleted(true);
        deliveryAgent.setUpdateDate(LocalDateTime.now());

        deliveryAgentsRepository.save(deliveryAgent);
    }

    @Override
    public void generatePassword(UUID deliveryAgentPublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository
                .findByPublicIdAndDeletedIsFalse(deliveryAgentPublicId);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with id " + deliveryAgentPublicId + " not found")
                    .userMessage("Sorry. Delivery agent with id " + deliveryAgentPublicId + " not found");
        }

        DeliveryAgents deliveryAgent = deliveryAgentOptional.get();

        personOptional.ifPresent(deliveryAgent::setUpdatedBy);
        deliveryAgent.setUpdateDate(LocalDateTime.now());

        String password = RandomStringUtils.randomAlphabetic(6);

        deliveryAgent.setPassword(passwordEncoder.encode(password));
        deliveryAgent.setForcePasswordReset(false);

        itibariSmsService.send(deliveryAgent.getPhoneNumber(), password);

        deliveryAgentsRepository.save(deliveryAgent);

        eventsRepository.save(new Events(deliveryAgentPublicId.toString(),
                "Password Generation",
                LocalDateTime.now(),
                "",
                "CREATE PASSWORD",
                personOptional.get()));
    }

    /*
    raise order amendments, ideally this should be done by the delivery agent
     */

    @Override
    public void amendOrder(UUID deliveryAgentPublicId, AmendOrderRequest amendOrderRequest, String name) {


        Optional<OrderAmendment> optionalOrderAmendment = orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(
                amendOrderRequest.getSalesOrderPublicId().toString());

        if (optionalOrderAmendment.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("An amendment request for sales order with id " + amendOrderRequest.getSalesOrderPublicId() + " already exists")
                    .userMessage("Sorry. An amendment request for sales order  already exists");
        }

        Optional<DeliveryAgents> optionalDeliveryAgents = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(deliveryAgentPublicId);

        if (optionalDeliveryAgents.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with id " + deliveryAgentPublicId + " not found")
                    .userMessage("Sorry. Delivery agent with id " + deliveryAgentPublicId + " not found");
        }

        if (amendOrderRequest.getAmendAction() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Amend action is required")
                    .userMessage("Sorry. Amend action is required");
        }

        DeliveryAgents deliveryAgent = optionalDeliveryAgents.get();

        Optional<RoutePlanning> routePlanningOptional = routePlanningRepository.findByPublicIdAndDeletedIsFalse(amendOrderRequest.getRoutePublicId());
        if (routePlanningOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id " + amendOrderRequest.getRoutePublicId() + " not found")
                    .userMessage("Sorry. Route  not found");
        }
        RoutePlanning routePlanning = routePlanningOptional.get();

        ProductRecon productRecon = productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(routePlanning.getRouteId()).orElse(null);

        if (productRecon == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing product recon")
                    .userMessage("Sorry. Missing product recon");
        }

        Optional<RejectionsCode> optionalRejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(
                amendOrderRequest.getReasonPublicId());

        if (optionalRejectionsCode.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Rejection code with id " + amendOrderRequest.getReasonPublicId() + " not found")
                    .userMessage("Sorry. Rejection code not found");
        }

        RejectionsCode rejectionsCode = optionalRejectionsCode.get();

        OrderAmendment orderAmendment = new OrderAmendment();

        Optional<SalesOrders> salesOrderOptional = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(amendOrderRequest.getSalesOrderPublicId());

        if (salesOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order with id " + amendOrderRequest.getSalesOrderPublicId() + " not found")
                    .userMessage("Sorry. Sales order not found");
        }

        SalesOrders salesOrder = salesOrderOptional.get();

        if (amendOrderRequest.getAmendAction() != AmendAction.RequestTransferVehicle) {

            if (amendOrderRequest.getSalesOrderPublicId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales order public id is required")
                        .userMessage("Sorry. Sales order public id is required");
            }

            salesOrder.setAwaitingApproval(true);
            salesOrder.setOrderAmendStatus(OrderAmendStatus.Pending);
            orderAmendment.setSalesOrder(salesOrder);
            salesOrdersRepository.save(salesOrder);
        }
        orderAmendment.setDescription(amendOrderRequest.getDescription());
        orderAmendment.setStatus(AmendmentStatus.Pending);
        orderAmendment.setRouteId(routePlanning.getRouteId());
        orderAmendment.setRoutePublicId(routePlanning.getPublicId().toString());
        orderAmendment.setAmendAction(amendOrderRequest.getAmendAction());
        orderAmendment.setDeliveryAgent(deliveryAgent);
        orderAmendment.setRejectionsCode(rejectionsCode);
        orderAmendment.setCreationTime(LocalDateTime.now());
        orderAmendment.setSalesOrderPublicId(amendOrderRequest.getSalesOrderPublicId().toString());
        OrderAmendment orderAmendment1 = orderAmendmentRepository.save(orderAmendment);
        if (!amendOrderRequest.getItems().isEmpty()) {
            Set<SalesOrderItems> orderItems = salesOrder.getSalesOrderItems();
            List<AmendmentProductItem> amendmentProductItems = new ArrayList<>();

            for (SalesOrderItems salesOrderItems : orderItems) {
                for (ItemRequest itemRequest : amendOrderRequest.getItems()) {
                    if (salesOrderItems.getPublicId().equals(itemRequest.getProductItemPublicId())) {
                        AmendmentProductItem amendmentProductItem = new AmendmentProductItem();
                        amendmentProductItem.setOrderAmendment(orderAmendment1);
                        amendmentProductItem.setAmendmentQuantity(salesOrderItems.getQuantity() - itemRequest.getQuantitySold()); // returned qty
                        amendmentProductItem.setProductCode(salesOrderItems.getProduct().getSku());
                        amendmentProductItem.setProductName(salesOrderItems.getProduct().getProductName());

                        amendmentProductItem.setIssuedQuantity(salesOrderItems.getQuantity());  // product item picked qty
                        amendmentProductItem.setDeliveredQuantity(itemRequest.getQuantitySold()); // product item delivered qty
                        amendmentProductItem.setPricePerUnit(salesOrderItems.getPricePerUnit());

                        //product quantity -

                        AmendmentProductItem amendmentProductItem1 = orderAmendmentItemRepository.save(amendmentProductItem);
                        amendmentProductItems.add(amendmentProductItem1);
                    }
                }
            }

            if (orderItems.size() != amendmentProductItems.size()) {
                // check left items
                Set<SalesOrderItems> leftItems = orderItems.stream()
                        .filter(item -> amendmentProductItems.stream()
                                .noneMatch(amendmentItem -> amendmentItem.getProductCode().equals(item.getProduct().getSku())))
                        .collect(Collectors.toSet());

                for (SalesOrderItems salesOrderItems : leftItems) {
                    // create amendment product item for left items
                    AmendmentProductItem amendmentProductItem = new AmendmentProductItem();
                    amendmentProductItem.setOrderAmendment(orderAmendment1);
                    amendmentProductItem.setAmendmentQuantity(salesOrderItems.getQuantity());
                    amendmentProductItem.setProductCode(salesOrderItems.getProduct().getSku());
                    amendmentProductItem.setProductName(salesOrderItems.getProduct().getProductName());
                    amendmentProductItem.setIssuedQuantity(salesOrderItems.getQuantity());
                    amendmentProductItem.setDeliveredQuantity(0);

                    amendmentProductItem.setPricePerUnit(salesOrderItems.getPricePerUnit());
                    AmendmentProductItem saved = orderAmendmentItemRepository.save(amendmentProductItem);
                    amendmentProductItems.add(saved);
                }
            }

            orderAmendment1.setAmendmentProductItems(amendmentProductItems);
        } else {
            Set<SalesOrderItems> orderItems = salesOrder.getSalesOrderItems();
            List<AmendmentProductItem> amendmentProductItems = new ArrayList<>();

            for (SalesOrderItems salesOrderItems : orderItems) {
                AmendmentProductItem amendmentProductItem = new AmendmentProductItem();
                amendmentProductItem.setOrderAmendment(orderAmendment1);
                amendmentProductItem.setAmendmentQuantity(salesOrderItems.getQuantity());
                amendmentProductItem.setProductCode(salesOrderItems.getProduct().getSku());
                amendmentProductItem.setProductName(salesOrderItems.getProduct().getProductName());
                amendmentProductItem.setIssuedQuantity(salesOrderItems.getQuantity());
                amendmentProductItem.setDeliveredQuantity(0);
                amendmentProductItem.setPricePerUnit(salesOrderItems.getPricePerUnit());

                AmendmentProductItem amendmentProductItem1 = orderAmendmentItemRepository.save(amendmentProductItem);
                amendmentProductItems.add(amendmentProductItem1);
            }
        }

        if ((amendOrderRequest.getItems().isEmpty())) {
            orderAmendment1.setFailedOrder(true);
        }

        OrderAmendment savedOrderAmendment = orderAmendmentRepository.save(orderAmendment1);
        tumaUjumbeKwaDepartment(savedOrderAmendment);

        String title = "Order Amendment " + savedOrderAmendment.getRouteId();
        String message = String.format("Order amendment request has been raised by %s %s for order %s with action %s. Description: %s",
                deliveryAgent.getFirstName(),
                deliveryAgent.getLastName(),
                savedOrderAmendment.getSalesOrder().getSalesOrderNumber(),
                savedOrderAmendment.getAmendAction().name(),
                savedOrderAmendment.getDescription());

        notificationHelper.createNotificationHelper(
                title,
                message,
                "ROLE_ADMIN",
                savedOrderAmendment.getPublicId(),
                "ORDER_AMENDMENT",
                "APPROVE_DELIVERY_AGENT");
    }

    @Override
    public Page<OrderAmendment> getAllOrderAmendments(String searchParam, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<OrderAmendment> cq = cb.createQuery(OrderAmendment.class);
        Root<OrderAmendment> root = cq.from(OrderAmendment.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));

        if (searchParam != null && searchParam.trim().length() >= 3) {
            List<Predicate> orPredicates = new ArrayList<>();
            orPredicates.add(cb.like(cb.upper(root.get("routeId")), "%" + searchParam.toUpperCase() + "%"));
            orPredicates.add(cb.like(cb.upper(root.get("salesOrder").get("salesOrderNumber")), "%" + searchParam.toUpperCase() + "%"));
            orPredicates.add(cb.like(cb.upper(root.get("salesOrder").get("customer").get("name")), "%" + searchParam.toUpperCase() + "%"));
            predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("id")));

        TypedQuery<OrderAmendment> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<OrderAmendment> resultList = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<OrderAmendment> countRoot = countQuery.from(OrderAmendment.class);
        countQuery.select(cb.countDistinct(countRoot));
        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.equal(countRoot.get("deleted"), Boolean.FALSE));

        if (searchParam != null && searchParam.trim().length() >= 3) {
            List<Predicate> orPredicates = new ArrayList<>();
            orPredicates.add(cb.like(cb.upper(countRoot.get("routeId")), "%" + searchParam.toUpperCase() + "%"));
            orPredicates.add(cb.like(cb.upper(countRoot.get("salesOrder").get("salesOrderNumber")), "%" + searchParam.toUpperCase() + "%"));
            orPredicates.add(cb.like(cb.upper(countRoot.get("salesOrder").get("customer").get("name")), "%" + searchParam.toUpperCase() + "%"));
            countPredicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
        }

        countQuery.where(countPredicates.toArray(new Predicate[0]));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, totalCount);
    }

    @Override
    public void amendOrderActions(AmendRequest amendRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        if (amendRequest.getAmendOrderPublicId() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Amend order public id is required")
                    .userMessage("Sorry. Order amendment missing parameters");
        }

        if (amendRequest.getActionType() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Action type is required")
                    .userMessage("Sorry. Action type is required");
        }

        if (amendRequest.getReasonCodePublicId() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing amendment reason")
                    .userMessage("Sorry. Missing amendment reason");
        }

        Optional<OrderAmendment> orderAmendmentOptional = orderAmendmentRepository.findByPublicIdAndDeletedIsFalse(amendRequest.getAmendOrderPublicId());
        if (orderAmendmentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Order amendment with id " + amendRequest.getAmendOrderPublicId() + " not found")
                    .userMessage("Sorry. Order amendment not found");
        }

        Optional<RejectionsCode> rejectionsCode =
                rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(amendRequest.getReasonCodePublicId());

        if (!rejectionsCode.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Amendment reason code not found")
                    .userMessage("Sorry. Amendment reason code not found");
        }

        OrderAmendment orderAmendment = orderAmendmentOptional.get();
        SalesOrders salesOrder = orderAmendment.getSalesOrder();

        if (amendRequest.getActionType() == ActionType.APPROVE) {
            orderAmendment.setStatus(AmendmentStatus.Approved);
            rejectionsCode.ifPresent(orderAmendment::setCxReasonCode);
            rejectionsCode.ifPresent(orderAmendment::setRejectionsCode);
            if (orderAmendment.getAmendAction() == AmendAction.CancelAndRequestVanSale) {
                salesOrder.setAwaitingApproval(false);
                salesOrder.setOrderAmendStatus(OrderAmendStatus.Approved);
                if (shouldAutoFailOnAmendmentApproval(orderAmendment, salesOrder)) {
                    applyFailedDeliveryOnAmendmentApproval(salesOrder, name, orderAmendment.isFailedOrder());
                }
            }
        } else if (amendRequest.getActionType() == ActionType.REJECT) {
            rejectionsCode.ifPresent(orderAmendment::setCxReasonCode);
            rejectionsCode.ifPresent(orderAmendment::setRejectionsCode);
            orderAmendment.setStatus(AmendmentStatus.Rejected);
            salesOrder.setAwaitingApproval(false);
            salesOrder.setOrderAmendStatus(OrderAmendStatus.Denied);
        }

        salesOrdersRepository.save(salesOrder);
        orderAmendment.setComment(amendRequest.getComment());
        orderAmendment.setUpdateDate(LocalDateTime.now());
        personOptional.ifPresent(orderAmendment::setUpdatedBy);
        orderAmendmentRepository.save(orderAmendment);
    }

    private VanSales getVansales(DeliveryAgents deliveryAgent, RoutePlanning routePlanning) {

        Optional<VanSales> optionalVanSales = Optional.ofNullable(vanSalesRepository.findByDeliveryAgents_PublicIdAndRoutePublicIdAndDeletedIsFalse(
                deliveryAgent.getPublicId(),
                String.valueOf(routePlanning.getPublicId())));

        if (optionalVanSales.isPresent()) {
            return optionalVanSales.get();
        }

        VanSales vanSales = new VanSales();
        vanSales.setDeliveryAgents(deliveryAgent);
        vanSales.setRoutePublicId(String.valueOf(routePlanning.getPublicId()));
        vanSales.setStatus(VanSaleStatus.Loaded);
        vanSales.setCreationTime(LocalDateTime.now());
        vanSales.setCreatedBy(routePlanning.getCreatedBy());
        return vanSalesRepository.save(vanSales);
    }

    @Override
    public DeliveryAgents getDeliveryAgentMobileProfile(String name) {

        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(name);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with phone number " + name + " not found")
                    .userMessage("Sorry. Delivery agent with phone number " + name + " not found");
        }
        return deliveryAgentOptional.get();
    }

    @Transactional
    @Override
    public void acceptRoute(UUID routeId, AcceptTypeEnum acceptType, String name) {

        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(name);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with phone number " + name + " not found")
                    .userMessage("Sorry. Delivery agent with phone number " + name + " not found");
        }

        DeliveryAgents deliveryAgent = deliveryAgentOptional.get();

        Optional<RoutePlanning> routePlanningOptional = routePlanningRepository.findByPublicIdAndDeletedIsFalse(routeId);

        if (routePlanningOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id " + routeId + " not found")
                    .userMessage("Sorry. Route  not found");
        }

        RoutePlanning routePlanning = routePlanningOptional.get();

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(routePlanning.getWarehouseId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Warehouse with id " + routePlanning.getWarehouseId() + " not found")
                        .userMessage("Sorry. Warehouse not found"));

        if (!routePlanning.getDeliveryAgentId().equalsIgnoreCase(deliveryAgent.getPublicId().toString())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id " + routeId + " does not belong to delivery agent with phone number " + name)
                    .userMessage("Sorry. Route  does not belong to delivery agent with phone number " + name);
        }

        if (routePlanning.getRouteStatus() != RouteStatus.Loaded) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id " + routeId + " is not pending")
                    .userMessage("Sorry. You cannot accept a route that is not loaded");
        }

        if (acceptType == AcceptTypeEnum.AcceptRoute) {
            routePlanning.setRouteStatus(RouteStatus.ReadyToGo);
        }

        routePlanning.setUpdateDate(LocalDateTime.now());

        Map<Product, Long> productLongMap = new HashMap<>();

        RoutePlanning savedRoutePlanning = routePlanningRepository.save(routePlanning);
        String description = String.format("Route %s has been %s by %s",
                savedRoutePlanning.getRouteId(),
                acceptType.name(),
                deliveryAgent.getFirstName() + " " + deliveryAgent.getLastName());
        eventsRepository.save(new Events(savedRoutePlanning.getDeliveryAgentId(),
                "Route Action",
                LocalDateTime.now(),
                description,
                acceptType.toString(),
                savedRoutePlanning.getCreatedBy()));

        Optional<ProductRecon> optionalProductRecon = productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(
                deliveryAgent.getPublicId().toString(),
                routePlanning.getRouteId());

        if (optionalProductRecon.isPresent()) {
            ProductRecon productRecon = optionalProductRecon.get();
            List<ProductItem> productItems = productItemRepository.findByProductReconAndDeletedIsFalse(productRecon);

            if (!productItems.isEmpty()) {
                productItems.forEach(productItem -> {

                    Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(productItem.getProductPublicId());
                    productOptional.ifPresent(product -> productLongMap.put(product, productItem.getQuantityPicked()));
                });
            }
        }

        stockUpdateHelper.updatePickedIssuedStock(productLongMap, warehouse, InventoryEvent.ISSUANCE.name());

        productLongMap.forEach((product, quantity) -> {
            stockMovementHelper.recordStockMovement(
                    product,
                    Math.toIntExact(quantity),
                    StockMovementTransactionType.ISSUANCE,
                    TransactionBoundType.OUTGOING,
                    "Delivery agent " + deliveryAgent.getFullName(),
                    warehouse

            );
        });
    }

    @Override
    public void routeActions(UUID routeId, RouteAction action, String name) {

        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(name);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with phone number " + name + " not found")
                    .userMessage("Sorry. Delivery agent with phone number " + name + " not found");
        }

        Optional<RoutePlanning> routePlanningOptional = routePlanningRepository.findByPublicIdAndDeletedIsFalse(routeId);
        if (routePlanningOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id " + routeId + " not found")
                    .userMessage("Sorry. Route not found");
        }
        RoutePlanning routePlanning = routePlanningOptional.get();

        DeliveryAgents deliveryAgent = deliveryAgentOptional.get();

        ProductRecon productRecon;
        Optional<ProductRecon> optionalProductRecon = productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(
                deliveryAgent.getPublicId().toString(),
                routePlanning.getRouteId());

        productRecon = optionalProductRecon.orElseGet(() -> warehouseService.reLoadProductRecon(deliveryAgent, routePlanning));

        if (productRecon == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product recon with delivery agent id " + deliveryAgent.getPublicId() + " and delivery id " + routePlanning.getRouteId() + " not found")
                    .userMessage("Sorry. Product recon not found");
        }

        if (action == RouteAction.Start) {
            if (routePlanning.getRouteStatus() != RouteStatus.ReadyToGo) {
                throw new BadRequestRestApiException()
                        .developerMessage("Route with id " + routeId + " is not ready to go")
                        .userMessage("Sorry. Route with id  is not ready to go");
            }
            routePlanning.getSalesOrders().forEach(order -> {
                order.setSalesOrderStatus(LoadingStatusEnum.IN_TRANSIT);
                order.setUpdateDate(LocalDateTime.now());
                salesOrdersRepository.save(order);
            });
            routePlanning.setRouteStatus(RouteStatus.IN_TRANSIT);
            routePlanning.setStartDateTime(LocalDateTime.now());
            productRecon.setStatus(ReconStatus.InProgress);
            productReconRepository.save(productRecon);
        } else if (action == RouteAction.end) {

            for (SalesOrders salesOrder : routePlanning.getSalesOrders()) {
                checkEndTrip(salesOrder);
            }

            if (routePlanning.getRouteStatus() != RouteStatus.IN_TRANSIT) {
                throw new BadRequestRestApiException()
                        .developerMessage("Route with id " + routeId + " is not in transit")
                        .userMessage("Sorry. Route " + routeId + " is not in transit");
            }

            if (productRecon.getStatus() != ReconStatus.InProgress) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product recon with delivery agent id " + deliveryAgent.getPublicId() +
                                " and delivery id " + routePlanning.getRouteId() + " is not in progress")
                        .userMessage("Sorry. Product recon for route " + routePlanning.getRouteId() + " is not in progress");
            }

            routePlanning.setRouteStatus(RouteStatus.DELIVERED);
            routePlanning.setEndDateTime(LocalDateTime.now());
            productRecon.setStatus(ReconStatus.ReadyForReconciliation);
            var savedProductRecon = productReconRepository.save(productRecon);

            warehouseService.updateReconDiscount(savedProductRecon, routePlanning.getPublicId().toString());

            // Keep the agent unavailable until final reconciliation is complete.
            logisticsHelper.updateVehicleAvailability(UUID.fromString(routePlanning.getVehicleId()), true);

        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Action " + action + " not supported")
                    .userMessage("Sorry. Action " + action + " not supported");
        }


        routePlanning.setUpdateDate(LocalDateTime.now());

        String description = String.format("Route %s has been %s by %s",
                routePlanning.getRouteId(),
                action.name(),
                deliveryAgent.getFirstName() + " " + deliveryAgent.getLastName());

        eventsRepository.save(new Events(routePlanning.getDeliveryAgentId(),
                "Route Action",
                LocalDateTime.now(),
                description,
                action.toString(),
                routePlanning.getCreatedBy()));

        routePlanningRepository.save(routePlanning);
    }

    public void checkEndTrip(SalesOrders salesOrders) {
        LoadingStatusEnum status = salesOrders.getSalesOrderStatus();
        DeliveryStatus deliveryStatus = salesOrders.getDeliveryStatus();
        String orderNumber = salesOrders.getSalesOrderNumber();
        String publicId = salesOrders.getPublicId().toString();

        if (status == LoadingStatusEnum.IN_TRANSIT) {
            throw new BadRequestRestApiException()
                    .developerMessage("Cannot complete route with status IN_TRANSIT for order " + orderNumber)
                    .userMessage("Sorry. Cannot complete route with status IN_TRANSIT for order " + orderNumber);
        }

        if (status == LoadingStatusEnum.DELIVERED) {
            switch (deliveryStatus) {
                case Delivered:
                    break;
                case PartiallyDelivered:
                case NotDelivered:
                case Failed:
                    checkIfNoActiveAmendments(publicId, orderNumber);
                    break;
                default:
                    throw new BadRequestRestApiException()
                            .developerMessage("Unexpected delivery status " + deliveryStatus + " for order " + orderNumber)
                            .userMessage("Sorry. Cannot complete route due to unexpected delivery status for order " + orderNumber);
            }
        }
    }

    private void checkIfNoActiveAmendments(String salesOrderPublicId, String salesOrderNumber) {
        orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(salesOrderPublicId)
                .filter(amendment -> AmendmentStatus.Pending.equals(amendment.getStatus()))
                .ifPresent(amendment -> {
                    throw new BadRequestRestApiException()
                            .developerMessage("Cannot complete route as there are pending order amendments for order " + salesOrderNumber)
                            .userMessage("Sorry. Cannot complete route as there are pending order amendments for order " + salesOrderNumber);
                });
    }


    private void recalculateDiscount(RoutePlanning routePlanning) {
        //null
    }

    @Override
    public void markOrderItemAsDelivered(DeliverItemRequest deliverItemRequest, String name) {

        log.debug("Marking order items as delivered for request: {}", deliverItemRequest);
        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(name);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with phone number " + name + " not found")
                    .userMessage("Sorry. Delivery agent  not found");
        }

        if (deliverItemRequest.getSalesOrderPublicId() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order public id is required")
                    .userMessage("Sorry. Sales order public id is required");
        }

        Optional<SalesOrders> salesOrdersOptional = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(deliverItemRequest.getSalesOrderPublicId());

        if (salesOrdersOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order with id " + deliverItemRequest.getSalesOrderPublicId() + " not found")
                    .userMessage("Sorry. Sales order not found");
        }

        SalesOrders salesOrders = salesOrdersOptional.get();

        Optional<RoutePlanning> routePlanningOptional = routePlanningRepository.findByPublicIdAndDeletedIsFalse(deliverItemRequest.getRoutePublicId());
        if (routePlanningOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id not found")
                    .userMessage("Sorry. Route  not found");
        }
        RoutePlanning routePlanning = routePlanningOptional.get();

        if (routePlanning.getRouteStatus() == RouteStatus.DELIVERED) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id " + routePlanning.getPublicId() + " has already been delivered")
                    .userMessage("Sorry. Route has already been delivered");
        }

        DeliveryAgents deliveryAgent = deliveryAgentOptional.get();

        Optional<ProductRecon> optionalProductRecon = productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(
                deliveryAgent.getPublicId().toString(),
                routePlanning.getRouteId());

        if (optionalProductRecon.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product recon with delivery agent id " + deliveryAgent.getPublicId() + " and delivery id " + routePlanning.getRouteId() + " not found")
                    .userMessage("Sorry. Product recon  not found");
        }
        ProductRecon productRecon = optionalProductRecon.get();
        if (!routePlanning.getDeliveryAgentId().equalsIgnoreCase(deliveryAgent.getPublicId().toString())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id  does not belong to delivery agent with phone number " + name)
                    .userMessage("Sorry. Route does not belong to delivery agent ");
        }

        if (deliverItemRequest.getItems() != null && !deliverItemRequest.getItems().isEmpty()) {

            log.info("markOrderItemAsDelivered: deliverItemRequest.getItems() is not null and not empty");

            deliverItemRequest.getItems().forEach(item -> {

                if (Validators.anyEqualNull(item.getProductItemPublicId(), item.getQuantitySold())) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Product item public id and quantity sold are required")
                            .userMessage("Sorry. Product item public id and quantity sold are required");
                }
                Optional<SalesOrderItems> salesOrderItemsOptional = salesOrderItemsRepository.findByPublicIdAndDeletedIsFalse(item.getProductItemPublicId());
                if (salesOrderItemsOptional.isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Sales order item with id " + item.getProductItemPublicId() + " not found")
                            .userMessage("Sorry. Sales order item  not found");
                }
                SalesOrderItems salesOrderItems = salesOrderItemsOptional.get();

                if (item.getQuantitySold() > 0) {
                    log.info("markOrderItemAsDelivered: item.getQuantitySold() is greater than 0");

                    if (salesOrderItems.getStatus() == OrderItemStatus.NotDelivered
                            || salesOrderItems.getStatus() == OrderItemStatus.PartiallyDelivered) {
                        int currentDeliveredQuantity = salesOrderItems.getQuantityDelivered();
                        int remainingQuantity = salesOrderItems.getQuantity() - currentDeliveredQuantity;
                        log.debug("Validating quantity - Order: {}, Remaining: {}, Requested: {}",
                                salesOrderItems.getQuantity(), remainingQuantity, item.getQuantitySold());

                        if (item.getQuantitySold() > remainingQuantity) {
                            throw new BadRequestRestApiException()
                                    .developerMessage("Quantity sold must be less than or equal to remaining quantity " + remainingQuantity)
                                    .userMessage("Sorry. Quantity sold must be less than or equal to remaining quantity " + remainingQuantity);
                        }

                        Optional<ProductItem> productItemOptional = productItemRepository.findByProductReconAndProductPublicIdAndDeletedIsFalse(
                                productRecon, salesOrderItems.getProduct().getPublicId());

                        if (productItemOptional.isEmpty()) {
                            throw new BadRequestRestApiException()
                                    .developerMessage("Product item with id " + item.getProductItemPublicId() + " not found")
                                    .userMessage("Sorry. Product item not found as order item");
                        }

                        ProductItem productItem = productItemOptional.get();
                        productItem.setQuantitySold(productItem.getQuantitySold() + item.getQuantitySold());
                        productItem.setOrderQuantity(productItem.getOrderQuantity() - item.getQuantitySold());
                        productItem.setUpdateDate(LocalDateTime.now());
                        ProductItem productItem1 = productItemRepository.save(productItem);

                        int newDeliveredQuantity = currentDeliveredQuantity + item.getQuantitySold();

                        if (newDeliveredQuantity == salesOrderItems.getQuantity()) {
                            salesOrderItems.setStatus(OrderItemStatus.Delivered);
                            salesOrderItems.setDeliveredDate(LocalDateTime.now());
                        } else {
                            salesOrderItems.setStatus(OrderItemStatus.PartiallyDelivered);
                        }
                        salesOrderItems.setQuantityDelivered(newDeliveredQuantity);
                        salesOrderItems.setUpdateDate(LocalDateTime.now());
                        SalesOrderItems savedItem = salesOrderItemsRepository.save(salesOrderItems);
                        updatePreBookedOperationQuantities(routePlanning, salesOrders, salesOrderItems, item.getQuantitySold());

                        if (salesOrderItems.getStatus() == OrderItemStatus.Delivered) {
                            String salesAgentPublicId = resolveSalesAgentPublicId(salesOrders);
                            if (salesAgentPublicId != null) {
                                createDeliveryAgentTrailTable(savedItem, salesAgentPublicId);
                            }
                        }
                        // update stock

                        liveStockHelper.updateLiveStockValue(UUID.fromString(productRecon.getWarehousePublicId()),
                                productItem1.getProductPublicId(),
                                (int) productItem1.getQuantitySold(),
                                ProductUpdateType.SOLD_STOCK,
                                LocalDate.now());


                        stockMovementHelper.recordStockMovement(
                                salesOrderItems.getProduct(),
                                item.getQuantitySold(),
                                StockMovementTransactionType.DELIVERY,
                                TransactionBoundType.RECORD,
                                "Delivery agent " + deliveryAgent.getFullName(),
                                salesOrders.getWarehouse()
                        );
                    }
                }
//                else{
//                    updatePreBookedOperationQuantities(routePlanning, salesOrders, salesOrderItems);
//                }
            });
            warehouseService.createReconciliation(productRecon, routePlanning.getPublicId().toString(), true);

            log.info("markOrderItemAsDelivered: routePlanning.getRouteStatus() is not DELIVERED");
        } else {
            log.info("markOrderItemAsDelivered: deliverItemRequest.getItems() is null or empty");
        }

        SalesOrders saved = salesOrdersRepository.save(salesOrders);
//
//        try {
//            //updateDiscountItems(saved, productRecon);
//        } catch (Exception e) {
//            log.error("Error updating discount items: {}", e.getMessage());
//        }

        log.info("Continuing with van sales check after reconciliation delay");

        checkVanSales(routePlanning, saved, productRecon);
    }

    private void updatePreBookedOperationQuantities(RoutePlanning routePlanning,
                                                    SalesOrders salesOrders,
                                                    SalesOrderItems salesOrderItems,
                                                    int quantitySold) {
        DeliveryAgentOperation deliveryAgentOperation = deliveryAgentOperationRepository
                .findByRouteIdAndOrderNumberAndSkuAndDeletedIsFalse(
                        routePlanning.getRouteId(),
                        salesOrders.getSalesOrderNumber(),
                        salesOrderItems.getProduct().getSku())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Delivery agent operation not found for route " + routePlanning.getRouteId()
                                + ", order " + salesOrders.getSalesOrderNumber()
                                + " and sku " + salesOrderItems.getProduct().getSku())
                        .userMessage("Sorry. Delivery agent operation not found"));

        deliveryAgentOperation.setPreBookedSoldQuantity(deliveryAgentOperation.getPreBookedSoldQuantity() + quantitySold);
        deliveryAgentOperation.setPreBookedVanQuantity(Math.max(0,
                salesOrderItems.getQuantity() - salesOrderItems.getQuantityDelivered()));
        deliveryAgentOperationRepository.save(deliveryAgentOperation);
    }

    private void updatePreBookedOperationQuantities(RoutePlanning routePlanning,
                                                    SalesOrders salesOrders) {
        salesOrders.getSalesOrderItems().stream()
                .filter(item -> item.getStatus() == OrderItemStatus.NotDelivered)
                .forEach(item -> {
                    DeliveryAgentOperation deliveryAgentOperation = deliveryAgentOperationRepository
                            .findByRouteIdAndOrderNumberAndSkuAndDeletedIsFalse(
                                    routePlanning.getRouteId(),
                                    salesOrders.getSalesOrderNumber(),
                                    item.getProduct().getSku())
                            .orElseThrow(() -> new BadRequestRestApiException()
                                    .developerMessage("Delivery agent operation not found for route "
                                            + routePlanning.getRouteId()
                                            + ", order " + salesOrders.getSalesOrderNumber()
                                            + " and sku " + item.getProduct().getSku())
                                    .userMessage("Sorry. Delivery agent operation not found"));

                    deliveryAgentOperation.setPreBookedVanQuantity(Math.max(0, item.getQuantity()));
                    deliveryAgentOperationRepository.save(deliveryAgentOperation);
                });
    }

    private void updateVanSaleSoldQuantities(RoutePlanning routePlanning, SalesOrders vanSaleOrder) {
        if (vanSaleOrder == null || vanSaleOrder.getSalesOrderItems() == null || vanSaleOrder.getSalesOrderItems().isEmpty()) {
            return;
        }

        buildVanSaleSoldOrderOperation(routePlanning,
                vanSaleOrder.getDeliveryAgent(),
                vanSaleOrder.getWarehouse().getPublicId(),
                vanSaleOrder);
    }

    private String resolveSalesAgentPublicId(SalesOrders salesOrders) {
        if (salesOrders.getSalesAgentPublicId() != null) {
            return salesOrders.getSalesAgentPublicId();
        }
        if (salesOrders.getCustomer() != null
                && salesOrders.getCustomer().getServedBy() != null) {
            return String.valueOf(salesOrders.getCustomer().getServedBy().getPublicId());
        }
        return null;
    }

    private void createDeliveryAgentTrailTable(SalesOrderItems savedItem, String salesAgentPublicId) {
        Optional<SalesAgents> salesAgentOptional = salesAgentsRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(salesAgentPublicId));
        if (salesAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent not found")
                    .userMessage("Sorry, Sales agent not found");
        }

        SalesOrderAgent salesOrderAgent = new SalesOrderAgent();
        salesOrderAgent.setSalesAgents(salesAgentOptional.get());
        SalesOrderAgent saveSalesOrderAgent = salesOrderAgentRepository.save(salesOrderAgent);

        SalesAgentDeliveredItem salesAgentDeliveredItem = new SalesAgentDeliveredItem();
        salesAgentDeliveredItem.setQuantity(savedItem.getQuantityDelivered());
        salesAgentDeliveredItem.setPricePerUnit(savedItem.getPricePerUnit());
        salesAgentDeliveredItem.setProductId(savedItem.getProduct().getPublicId());
        salesAgentDeliveredItem.setOrderNumber(savedItem.getSalesOrder().getSalesOrderNumber());
        salesAgentDeliveredItem.setSalesOrderAgent(saveSalesOrderAgent);


        salesAgentDeliveredItemRepository.save(salesAgentDeliveredItem);
    }

    private void updateDiscountItems(SalesOrders saved, ProductRecon productRecon) {

        List<SalesOrderItems> discountItems = saved.getSalesOrderItems().stream()
                .filter(SalesOrderItems::isHasDiscount)
                .toList();
        if (!discountItems.isEmpty()) {
            for (SalesOrderItems item : discountItems) {
                log.info("Creating discount item for order item: {}", item.getPublicId());

                Optional<ProductItem> productItemOptional = productItemRepository.findByProductReconAndProductPublicIdAndDeletedIsFalse(
                        productRecon, item.getProduct().getPublicId());

                if (productItemOptional.isPresent()) {
                    ProductItem productItem = productItemOptional.get();

                    DiscountItems discountItem = discountItemsRepository
                            .findDiscountItemsBySalesOrderItemPublicIdAndDeletedIsFalse(item.getPublicId().toString())
                            .orElse(new DiscountItems());
                    discountItem.setSalesOrderPublicId(saved.getPublicId().toString());
                    discountItem.setSalesOrderItemPublicId(item.getPublicId().toString());
                    discountItem.setProductPublicId(item.getProduct().getPublicId().toString());
                    discountItem.setCustomerPublicId(saved.getCustomer().getPublicId().toString());
                    discountItem.setDiscountAmount(item.getDiscount());
                    discountItem.setProductReconPublicId(productRecon.getPublicId().toString());
                    discountItem.setProductReconItemPublicId(productItem.getPublicId().toString());
                    discountItemsRepository.save(discountItem);

                    log.info("Discount item created for order item: {}", item.getPublicId());
                }
            }
        }
    }

    @Override
    public void markVanSaleItemAsSold(DeliverVanSaleItemRequest deliverItemRequest, String name) {

        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(name);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with phone number " + name + " not found")
                    .userMessage("Sorry. Delivery agent  not found");
        }

        if (Validators.anyEqualNull(deliverItemRequest.getItems(), deliverItemRequest.getRoutePublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product item public id, route public id and quantity sold are required")
                    .userMessage("Sorry. Product item public id, route public id and quantity sold are required");
        }

        if (deliverItemRequest.getItems().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product item public id, route public id and quantity sold are required")
                    .userMessage("Sorry. Product item public id, route public id and quantity sold are required");
        }

        deliverItemRequest.getItems().forEach(item -> {
            if (Validators.anyEqualNull(item.getProductItemPublicId(), item.getQuantitySold())) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product item public id and quantity sold are required")
                        .userMessage("Sorry. Product item public id and quantity sold are required");
            }
        });

        // create customer
        DeliveryAgents deliveryAgent = deliveryAgentOptional.get();

        Customers customer = customersService.createCustomerFromVanSale(deliverItemRequest, deliveryAgent.getCreatedBy());

        Optional<RoutePlanning> routePlanningOptional = routePlanningRepository.findByPublicIdAndDeletedIsFalse(deliverItemRequest.getRoutePublicId());
        if (routePlanningOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id not found")
                    .userMessage("Sorry. Route  not found");
        }
        RoutePlanning routePlanning = routePlanningOptional.get();

        if (routePlanning.getRouteStatus() == RouteStatus.DELIVERED) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id " + routePlanning.getPublicId() + " has already been delivered")
                    .userMessage("Sorry. Route has already been delivered");
        }

        Optional<ProductRecon> optionalProductRecon = productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(
                deliveryAgent.getPublicId().toString(),
                routePlanning.getRouteId());

        if (optionalProductRecon.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product recon with delivery agent id " + deliveryAgent.getPublicId() + " and delivery id " + routePlanning.getRouteId() + " not found")
                    .userMessage("Sorry. Product recon  not found");
        }
        ProductRecon productRecon = optionalProductRecon.get();
        if (!routePlanning.getDeliveryAgentId().equalsIgnoreCase(deliveryAgent.getPublicId().toString())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id  does not belong to delivery agent with phone number " + name)
                    .userMessage("Sorry. Route does not belong to delivery agent ");
        }

        List<ItemRequest> itemRequests = new ArrayList<>();

        deliverItemRequest.getItems().forEach(item -> {
            if (item.getQuantitySold() > 0) {
                Optional<ProductItem> productItemOptional = productRecon.getProductItems().stream()
                        .filter(ProductItem::isVanSaleItem)
                        .filter(productItem -> productItem.getPublicId().equals(item.getProductItemPublicId()))
                        .findFirst();

                if (productItemOptional.isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Product item with id " + item.getProductItemPublicId() + " not found")
                            .userMessage("Sorry. Product item not found as van sale item");
                }

                ProductItem productItem = productItemOptional.get();
                if (item.getQuantitySold() > productItem.getQuantityPicked() - productItem.getQuantitySold()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Quantity sold must be less than or equal to " + productItem.getQuantityPicked())
                            .userMessage("Sorry. Quantity sold must be less than or equal to " + productItem.getVanSaleQuantity());
                }
                productItem.setQuantitySold(productItem.getQuantitySold() + item.getQuantitySold());
                productItem.setVanSaleQuantity(productItem.getVanSaleQuantity() - item.getQuantitySold());
                productItem.setUpdateDate(LocalDateTime.now());
                productItem.setVanSaleItemSold(productItem.getVanSaleItemSold() + item.getQuantitySold());
                ProductItem productItem1 = productItemRepository.save(productItem);

                itemRequests.add(new ItemRequest(productItem1.getProductPublicId(), item.getQuantitySold(), productItem1.getPublicId()));

                liveStockHelper.updateLiveStockValue(UUID.fromString(productRecon.getWarehousePublicId()),
                        productItem1.getProductPublicId(),
                        (int) productItem1.getQuantitySold(),
                        ProductUpdateType.SOLD_STOCK,
                        LocalDate.now());

                Product product = productRepository.findByPublicIdAndDeletedIsFalse(productItem1.getProductPublicId())
                        .orElseThrow(() -> new BadRequestRestApiException()
                                .developerMessage("Product with id " + productItem1.getProductPublicId() + " not found")
                                .userMessage("Sorry. Product not found"));

                stockMovementHelper.recordStockMovement(
                        product,
                        item.getQuantitySold(),
                        StockMovementTransactionType.DELIVERY,
                        TransactionBoundType.RECORD,
                        "Delivery agent " + deliveryAgent.getFullName(),
                        deliveryAgent.getWarehouse());
            }
        });

        warehouseService.createReconciliation(productRecon, routePlanning.getPublicId().toString(), true);

        SalesOrders vanSaleOrder = salesOrdersService.updateNewCustomerOrders(customer, itemRequests, routePlanning, productRecon.getPublicId());
        vanSaleOrder.setDeliveryAgent(deliveryAgent);

        SalesOrders saveSalesOrder = salesOrdersRepository.save(vanSaleOrder);
        updateVanSaleSoldQuantities(routePlanning, vanSaleOrder);
    }

    private static final String EVENT_CASH_TO_CREDIT_AT_DELIVERY = "CASH_TO_CREDIT_AT_DELIVERY";
    private static final String EVENT_CASH_TO_CREDIT_POST_DELIVERY = "CASH_TO_CREDIT_POST_DELIVERY";
    private static final int MAX_CONVERSION_REASON_LENGTH = 500;

    @Override
    @Transactional
    public void markOrderAsDelivered(UUID orderPublicId, UUID routePublicId, MarkOrderDeliveredRequest request, String name) throws IOException, InterruptedException {

        MarkOrderDeliveredRequest effectiveRequest = request != null ? request : new MarkOrderDeliveredRequest();
        boolean conversionRequested = effectiveRequest.isConvertCashToCredit();

        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(name);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with phone number " + name + " not found")
                    .userMessage("Sorry. Delivery agent not found");
        }

        DeliveryAgents deliveryAgents = deliveryAgentOptional.get();

        Optional<SalesOrders> salesOrderOptional = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(orderPublicId);
        if (salesOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order with id " + orderPublicId + " not found")
                    .userMessage("Sorry. Sales order not found");
        }

        Optional<OrderAmendment> orderAmendmentOptional = orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(orderPublicId.toString());

        if (orderAmendmentOptional.isPresent()) {
            OrderAmendment orderAmendment = orderAmendmentOptional.get();

            if (orderAmendment.getAmendAction() == AmendAction.CancelAndRequestVanSale &&
                    orderAmendment.getStatus() == AmendmentStatus.Pending) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales order with id " + orderPublicId + " has an approved amendment for cancellation and van sale")
                        .userMessage("Sorry. Sales order cannot be marked as delivered as it has a pending amendment for cancellation and" +
                                " van sale");
            }
        }

        SalesOrders salesOrder = salesOrderOptional.get();

        handleCashToCreditConversionAtDelivery(salesOrder, effectiveRequest, deliveryAgents, routePublicId);

        if (salesOrder.getSalesOrderStatus() != LoadingStatusEnum.FAILED_DELIVERY) {
            salesOrder.setSalesOrderStatus(LoadingStatusEnum.DELIVERED);
            salesOrder.setSoldAt(LocalDateTime.now());

            // Preserve existing credit order header payment convention; only cash orders are marked PAID here.
            if (salesOrder.getOrderType() == OrderType.CASH && !salesOrder.isCreditApplied()) {
                salesOrder.setPaymentStatus(PaymentStatusEnum.PAID);
            }
            try {
                updateCustomerLastOrderDate(salesOrder.getCustomer().getPublicId(), salesOrder.getSoldAt());
            } catch (Exception e) {
                log.info("====Error from {} {} and message {}====", salesOrder.getCustomer().getPublicId(), salesOrder.getSoldAt(), e.getMessage());
            }
        }

        salesOrder.setUpdateDate(LocalDateTime.now());
        salesOrder.setDeliveryDate(LocalDateTime.now());

        // Persist delivered GROSS (price × qty delivered). getTotalPayable() is already net of
        // line discount; writing that here made IMS ORDER TOTAL equal TOTAL PAYABLE after delivery.
        salesOrder.setTotalAmount(salesOrder.getTotalAmountDelivered());

        String description = String.format("Order marked as delivered by %s for order %s",
                deliveryAgents.getFullName(),
                salesOrder.getSalesOrderNumber());

        eventsRepository.saveAndFlush(new Events(salesOrder.getPublicId().toString(),
                "Order Delivered",
                LocalDateTime.now(),
                description,
                "Order Delivered",
                null));

        SalesOrders saved = salesOrdersRepository.saveAndFlush(salesOrder);
        updateOrderStatusAndDeliveredAmount(saved);
        saved = ensureDeliveredStatusIsPersisted(saved);

        if (saved.getSalesOrderStatus() == LoadingStatusEnum.DELIVERED) {
//            sendSmsToMarkOrderAsDelivered(saved.getCustomer());

            String customer_name = saved.getCustomer().getStoreName() != null
                    ? saved.getCustomer().getStoreName()
                    : saved.getCustomer().getName();

            Customers customers = salesOrder.getCustomer();
            try {
                tumaMessage.whatsAppCustomer(LoadingStatusEnum.DELIVERED.name(), saved.getCustomer().getPhoneNumber(), customer_name, saved.getSalesOrderNumber());
                if (customers.isHasParentCustomer()) {
                    String parent_name = customers.getParentCustomer().getStoreName() != null
                            ? saved.getCustomer().getStoreName()
                            : saved.getCustomer().getName();
                    tumaMessage.whatsAppCustomer(LoadingStatusEnum.DELIVERED.name(), customers.getParentCustomer().getPhoneNumber(), parent_name, saved.getSalesOrderNumber());
                }
            } catch (Exception e) {
                log.error("Error sending WhatsApp delivery notification for order {}",
                        saved.getSalesOrderNumber(), e);
            }
        }

        try {
            sendCustomerDeliveryNotification(saved, deliveryAgents.getFullName());
        } catch (Exception e) {
            log.error("Error sending customer delivery notification for order {}",
                    saved.getSalesOrderNumber(), e);
        }

        RoutePlanning routePlanning = null;
        try {
            List<RoutePlanning> routePlanningList = routePlanningRepository.findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusesAndDeletedIsFalse(
                    String.valueOf(deliveryAgents.getPublicId()),
                    Collections.singletonList(RouteStatus.IN_TRANSIT));

            if (routePlanningList.size() == 1) {
                routePlanning = routePlanningList.get(0);
                discountHelper.updateCreateDiscounts(routePlanning);
            } else {
                log.warn("[DISCOUNT-TRACE] discount recalc SKIPPED on delivery: agent={} order={} inTransitRoutes={} (recalc only runs when exactly 1)",
                        deliveryAgents.getPublicId(), saved.getSalesOrderNumber(), routePlanningList.size());
            }
        } catch (Exception e) {
            log.error("[DISCOUNT-TRACE] Error updating product recon discount after delivery for order {}",
                    saved.getSalesOrderNumber(), e);
        }

        // try updating discount
        try {
            if (saved.isHasSpecialDiscount()) {
                var invoice = saved.getInvoice();
                if (invoice != null) {
                    var discountPublicId = invoice.getDiscountPublicId();

                    Optional<Discounts> optionalDiscounts = discountRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(
                            discountPublicId));

                    if (optionalDiscounts.isPresent() && optionalDiscounts.get().getStatus() == DiscountStatus.Active) {
                        Discounts discount = optionalDiscounts.get();
                        discount.setStatus(DiscountStatus.InActive);
                        discountRepository.save(discount);
                        log.info("[DISCOUNT-TRACE] special discount {} deactivated after delivery of order {}",
                                discount.getPublicId(), saved.getSalesOrderNumber());
                    }
                }
            }
        } catch (Exception e) {
            log.error("[DISCOUNT-TRACE] Error deactivating special discount for order {}", saved.getSalesOrderNumber(), e);
        }

        if (saved.getOrderType() == OrderType.CREDIT) {
            try {
                updateCreditAmountOnDelivery(saved);

                if (conversionRequested) {
                    refreshReconciliationAndRouteBalance(deliveryAgents, saved, routePublicId);
                }
            } catch (Exception e) {
                log.error("Error updating credit amount on delivery for order {}",
                        saved.getSalesOrderNumber(), e);
            }
        }

        try {
            updateProductItemsDiscounts(routePlanning);
        } catch (Exception e) {
            log.error("[DISCOUNT-TRACE] Error updating product item discounts after delivery for order {}",
                    saved.getSalesOrderNumber(), e);
        }

        try {
            if (saved.getOrderType() == OrderType.CREDIT && saved.getEnterpriseOrder() != null && saved.getEnterpriseOrder() == Boolean.TRUE) {
                log.info("enterprise no tra now ");
            } else {

                if (saved.getCustomer().getTin() != null && !saved.getCustomer().getTin().isEmpty()) {
                    if (!customerCheckerHelper.validateCustomerTinNumber(salesOrder.getCustomer())) {
                        return;
                    }
                }
                this.generateTraCustomerReceipt(saved);
            }

        } catch (Exception e) {
            log.error("Error generating TRA receipt on delivery for order {}",
                    saved.getSalesOrderNumber(), e);
        }
    }

    private SalesOrders ensureDeliveredStatusIsPersisted(SalesOrders salesOrder) {
        SalesOrders latest = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrder.getPublicId())
                .orElse(salesOrder);

        if (latest.getSalesOrderStatus() == LoadingStatusEnum.FAILED_DELIVERY) {
            return latest;
        }

        boolean changed = latest.getSalesOrderStatus() != LoadingStatusEnum.DELIVERED;
        latest.setSalesOrderStatus(LoadingStatusEnum.DELIVERED);

        if (latest.getSoldAt() == null) {
            latest.setSoldAt(LocalDateTime.now());
            changed = true;
        }

        if (latest.getDeliveryDate() == null) {
            latest.setDeliveryDate(LocalDateTime.now());
            changed = true;
        }

        if (latest.getOrderType() == OrderType.CASH
                && !latest.isCreditApplied()
                && latest.getPaymentStatus() != PaymentStatusEnum.PAID) {
            latest.setPaymentStatus(PaymentStatusEnum.PAID);
            changed = true;
        }

        OrderStatusEnum previousStatus = latest.getStatus();
        if (latest.areAllItemsDelivered()) {
            latest.setStatus(OrderStatusEnum.Delivered);
        } else {
            latest.setStatus(OrderStatusEnum.Partially_Delivered);
        }

        if (previousStatus != latest.getStatus()) {
            changed = true;
        }

        if (changed) {
            latest.setUpdateDate(LocalDateTime.now());
            return salesOrdersRepository.saveAndFlush(latest);
        }
        return latest;
    }

    private void handleCashToCreditConversionAtDelivery(SalesOrders salesOrder,
                                                        MarkOrderDeliveredRequest request,
                                                        DeliveryAgents deliveryAgents,
                                                        UUID routePublicId) {
        if (!request.isConvertCashToCredit()) {
            return;
        }

        BigDecimal availableCredit = validateAndPrepareCashToCreditConversion(
                salesOrder, request.getConversionReason(), true);

        if (routePublicId != null) {
            resolveRoutePlanningForOrder(deliveryAgents, salesOrder, routePublicId);
        }

        applyCashToCreditConversion(
                salesOrder,
                request.getConversionReason(),
                availableCredit,
                deliveryAgents != null ? deliveryAgents.getPublicId() : null,
                routePublicId,
                EVENT_CASH_TO_CREDIT_AT_DELIVERY,
                "at delivery");
    }

    @Override
    @Transactional
    public void convertDeliveredCashOrderToCredit(UUID orderPublicId,
                                                  ConvertCashToCreditRequest request,
                                                  String name) {
        SalesOrders salesOrder = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(orderPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sales order with id " + orderPublicId + " not found")
                        .userMessage("Sorry. Sales order not found"));

        if (salesOrder.getSalesOrderStatus() != LoadingStatusEnum.DELIVERED
                && salesOrder.getStatus() != OrderStatusEnum.Delivered
                && salesOrder.getStatus() != OrderStatusEnum.Partially_Delivered) {
            throw new BadRequestRestApiException()
                    .developerMessage("Order " + salesOrder.getSalesOrderNumber() + " is not delivered")
                    .userMessage("Only delivered orders can be converted to credit after delivery");
        }

        BigDecimal availableCredit = validateAndPrepareCashToCreditConversion(
                salesOrder, request.getConversionReason(), false);

        DeliveryAgents deliveryAgents = salesOrder.getDeliveryAgent();
        UUID routePublicId = resolveRoutePublicIdForPostDeliveryConversion(salesOrder, request.getRoutePublicId());

        applyCashToCreditConversion(
                salesOrder,
                request.getConversionReason(),
                availableCredit,
                deliveryAgents != null ? deliveryAgents.getPublicId() : null,
                routePublicId,
                EVENT_CASH_TO_CREDIT_POST_DELIVERY,
                "post delivery by " + name);

        SalesOrders saved = salesOrdersRepository.saveAndFlush(salesOrder);

        try {
            updateCreditAmountOnDelivery(saved);
        } catch (Exception e) {
            log.error("Error creating credit items during post-delivery cash-to-credit for order {}",
                    saved.getSalesOrderNumber(), e);
            throw new BadRequestRestApiException()
                    .developerMessage("Failed to create credit items for order " + saved.getSalesOrderNumber()
                            + ": " + e.getMessage())
                    .userMessage("Order could not be converted to credit. Please try again or contact support");
        }

        if (deliveryAgents != null && routePublicId != null) {
            try {
                refreshReconciliationAndRouteBalance(deliveryAgents, saved, routePublicId);
            } catch (Exception e) {
                log.warn("Cash-to-credit post-delivery succeeded for order {} but recon refresh failed: {}",
                        saved.getSalesOrderNumber(), e.getMessage());
            }
        } else {
            log.warn("Cash-to-credit post-delivery for order {} skipped recon refresh (da={}, route={})",
                    saved.getSalesOrderNumber(),
                    deliveryAgents != null ? deliveryAgents.getPublicId() : null,
                    routePublicId);
        }
    }

    private BigDecimal validateAndPrepareCashToCreditConversion(SalesOrders salesOrder,
                                                                String conversionReason,
                                                                boolean atDelivery) {
        if (conversionReason == null || conversionReason.trim().length() < 3) {
            throw new BadRequestRestApiException()
                    .developerMessage("conversionReason is required when converting cash to credit")
                    .userMessage("A conversion reason is required to convert this order to credit");
        }

        // At delivery we require explicit CASH. Post-delivery also allows null orderType
        // (unpaid delivered orders that never flipped to PAID) and CREDIT without creditApplied
        // so a broken booking state can be repaired without re-delivery.
        if (atDelivery) {
            if (salesOrder.getOrderType() != OrderType.CASH) {
                throw new BadRequestRestApiException()
                        .developerMessage("Order " + salesOrder.getSalesOrderNumber() + " is not a cash order")
                        .userMessage("Only cash orders can be converted to credit at delivery");
            }
        } else {
            OrderType orderType = salesOrder.getOrderType();
            boolean convertible = orderType == null
                    || orderType == OrderType.CASH
                    || (orderType == OrderType.CREDIT && !salesOrder.isCreditApplied());
            if (!convertible) {
                throw new BadRequestRestApiException()
                        .developerMessage("Order " + salesOrder.getSalesOrderNumber()
                                + " has orderType=" + orderType + ", creditApplied=" + salesOrder.isCreditApplied())
                        .userMessage("Only unpaid cash (or incomplete credit) orders can be converted to credit");
            }
        }

        if (salesOrder.isCreditApplied()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Order " + salesOrder.getSalesOrderNumber() + " already has credit applied")
                    .userMessage("This order is already on credit");
        }

        if (salesOrder.getPaymentStatus() == PaymentStatusEnum.PAID) {
            throw new BadRequestRestApiException()
                    .developerMessage("Order " + salesOrder.getSalesOrderNumber() + " is already marked as paid")
                    .userMessage("This cash order has already been paid and cannot be converted to credit");
        }

        creditLimitItemRepository.findByOrderNumberAndStatusAndDeletedIsFalse(
                        salesOrder.getSalesOrderNumber(),
                        WalletItemStatus.ACTIVE)
                .ifPresent(item -> {
                    throw new BadRequestRestApiException()
                            .developerMessage("Active credit item already exists for order " + salesOrder.getSalesOrderNumber())
                            .userMessage("This order already has an active credit record");
                });

        BigDecimal delivered = Optional.ofNullable(salesOrder.getTotalAmountDelivered()).orElse(BigDecimal.ZERO);
        BigDecimal discount = Optional.ofNullable(salesOrder.getTotalDiscount()).orElse(BigDecimal.ZERO);
        BigDecimal deliveredPayable = delivered.subtract(discount);
        if (deliveredPayable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivered payable is zero for order " + salesOrder.getSalesOrderNumber())
                    .userMessage("Cannot convert to credit until items have been delivered");
        }

        BigDecimal availableCredit = customerCheckerHelper.checkIfCustomerIsAllowedToBuyOnCredit(
                salesOrder.getCustomer().getPublicId(), salesOrder.isBookedByParent());

        if (availableCredit.compareTo(deliveredPayable) < 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Insufficient credit for order " + salesOrder.getSalesOrderNumber()
                            + ": available=" + availableCredit + ", required=" + deliveredPayable)
                    .userMessage("Customer does not have enough available credit for this delivered order");
        }

        return availableCredit;
    }

    private void applyCashToCreditConversion(SalesOrders salesOrder,
                                             String conversionReason,
                                             BigDecimal availableCredit,
                                             UUID deliveryAgentPublicId,
                                             UUID routePublicId,
                                             String eventType,
                                             String contextLabel) {
        BigDecimal delivered = Optional.ofNullable(salesOrder.getTotalAmountDelivered()).orElse(BigDecimal.ZERO);
        BigDecimal discount = Optional.ofNullable(salesOrder.getTotalDiscount()).orElse(BigDecimal.ZERO);
        BigDecimal deliveredPayable = delivered.subtract(discount);

        OrderType previousType = salesOrder.getOrderType();
        salesOrder.setOrderType(OrderType.CREDIT);
        salesOrder.setCreditApplied(true);
        salesOrder.setPaymentStatus(PaymentStatusEnum.NOT_PAID);
        salesOrder.setUpdateDate(LocalDateTime.now());

        String sanitizedReason = sanitizeConversionReason(conversionReason);
        String eventDescription = String.format(
                "Cash order converted to credit %s. order=%s prevType=%s newType=CREDIT reason=%s deliveredPayable=%s availableCredit=%s daPublicId=%s routePublicId=%s",
                contextLabel,
                salesOrder.getSalesOrderNumber(),
                previousType,
                sanitizedReason,
                deliveredPayable,
                availableCredit,
                deliveryAgentPublicId,
                routePublicId);

        eventsRepository.save(new Events(
                salesOrder.getPublicId().toString(),
                eventType,
                LocalDateTime.now(),
                eventDescription,
                eventType,
                null));

        log.info("cash_to_credit_{} orderPublicId={} orderNumber={} deliveredPayable={} availableCredit={} routePublicId={}",
                contextLabel.replace(' ', '_'),
                salesOrder.getPublicId(),
                salesOrder.getSalesOrderNumber(),
                deliveredPayable,
                availableCredit,
                routePublicId);
    }

    private UUID resolveRoutePublicIdForPostDeliveryConversion(SalesOrders salesOrder, UUID requestedRoutePublicId) {
        if (requestedRoutePublicId != null) {
            return requestedRoutePublicId;
        }
        if (salesOrder.getRoutePublicId() != null && !salesOrder.getRoutePublicId().isBlank()) {
            try {
                return UUID.fromString(salesOrder.getRoutePublicId());
            } catch (IllegalArgumentException ignored) {
                log.warn("Order {} has non-UUID routePublicId={}", salesOrder.getSalesOrderNumber(),
                        salesOrder.getRoutePublicId());
            }
        }
        return null;
    }

    private String sanitizeConversionReason(String reason) {
        String trimmed = reason.trim();
        if (trimmed.length() <= MAX_CONVERSION_REASON_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_CONVERSION_REASON_LENGTH);
    }

    private void refreshReconciliationAndRouteBalance(DeliveryAgents deliveryAgents,
                                                      SalesOrders salesOrder,
                                                      UUID routePublicId) {
        RoutePlanning routePlanning = resolveRoutePlanningForOrder(deliveryAgents, salesOrder, routePublicId);

        Optional<ProductRecon> productReconOptional = productReconRepository
                .findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(
                        deliveryAgents.getPublicId().toString(),
                        routePlanning.getRouteId());

        if (productReconOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product recon not found for route " + routePlanning.getPublicId())
                    .userMessage("Unable to refresh delivery reconciliation for this route");
        }

        ProductRecon productRecon = productReconOptional.get();
        warehouseService.createReconciliation(productRecon, routePlanning.getPublicId().toString(), true);

        Reconciliation reconciliation = reconciliationRepository
                .findByRoutePublicIdAndDeletedIsFalse(routePlanning.getPublicId().toString())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Reconciliation not found after refresh for route " + routePlanning.getPublicId())
                        .userMessage("Unable to refresh delivery reconciliation for this route"));

        BigDecimal deliveryAmount = reconciliation.getDeliveryAmount() != null
                ? reconciliation.getDeliveryAmount() : BigDecimal.ZERO;
        BigDecimal depositAmount = reconciliation.getDepositAmount() != null
                ? reconciliation.getDepositAmount() : BigDecimal.ZERO;
        BigDecimal creditAmount = reconciliation.getCreditAmount() != null
                ? reconciliation.getCreditAmount() : BigDecimal.ZERO;

        routePlanning.setBalance(deliveryAmount.subtract(depositAmount.add(creditAmount)));
        routePlanning.setUpdateDate(LocalDateTime.now());
        routePlanningRepository.save(routePlanning);
    }

    private RoutePlanning resolveRoutePlanningForOrder(DeliveryAgents deliveryAgents,
                                                       SalesOrders salesOrder,
                                                       UUID routePublicId) {
        if (routePublicId != null) {
            RoutePlanning routePlanning = routePlanningRepository.findByPublicIdAndDeletedIsFalse(routePublicId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Route planning with id " + routePublicId + " not found")
                            .userMessage("Sorry. Route not found"));

            if (!orderBelongsToRoute(salesOrder, routePlanning)) {
                throw new BadRequestRestApiException()
                        .developerMessage("Order " + salesOrder.getPublicId() + " is not on route " + routePublicId)
                        .userMessage("This order is not assigned to the specified route");
            }

            if (!routePlanning.getDeliveryAgentId().equalsIgnoreCase(deliveryAgents.getPublicId().toString())) {
                throw new BadRequestRestApiException()
                        .developerMessage("Route " + routePublicId + " does not belong to delivery agent " + deliveryAgents.getPublicId())
                        .userMessage("Sorry. This route does not belong to you");
            }

            return routePlanning;
        }

        List<RoutePlanning> matchingRoutes = routePlanningRepository
                .findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusesAndDeletedIsFalse(
                        String.valueOf(deliveryAgents.getPublicId()),
                        Collections.singletonList(RouteStatus.IN_TRANSIT))
                .stream()
                .filter(route -> orderBelongsToRoute(salesOrder, route))
                .toList();

        if (matchingRoutes.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No in-transit route found for order " + salesOrder.getPublicId())
                    .userMessage("Unable to determine the delivery route for reconciliation refresh");
        }

        if (matchingRoutes.size() > 1) {
            throw new BadRequestRestApiException()
                    .developerMessage("Multiple in-transit routes contain order " + salesOrder.getPublicId())
                    .userMessage("Unable to determine the delivery route for reconciliation refresh");
        }

        return matchingRoutes.get(0);
    }

    private boolean orderBelongsToRoute(SalesOrders salesOrder, RoutePlanning routePlanning) {
        if (routePlanning.getSalesOrders() == null) {
            return false;
        }
        return routePlanning.getSalesOrders().stream()
                .anyMatch(routeOrder -> routeOrder.getPublicId().equals(salesOrder.getPublicId()));
    }

    private void updateCustomerLastOrderDate(UUID customerPublicId, LocalDateTime soldAt) {
        customersRepository.updateLastOrderDate(
                customerPublicId,
                soldAt
        );
    }


    private void sendSmsToMarkOrderAsDelivered(Customers customer) {
        String name = customer.getStoreName() != null
                ? customer.getStoreName()
                : customer.getName();

        String formattedMessage = String.format("%s, Oda yako imefikishwa kikamilifu. Tafadhali pakua risiti yako kupitia Itibari Customer App. Asante kwa kuichagua Itibari.", name);
        tumaMessage.tumaUjumbeKwaMteja(customer.getPhoneNumber(), formattedMessage, customer.getFullName());
    }

    private void updateOrderStatusAndDeliveredAmount(SalesOrders salesOrder) {
        salesOrder.setDeliveredAmount(salesOrder.getTotalAmountDelivered());
        salesOrder.setStaticDiscount(salesOrder.getTotalDiscount());

        if (salesOrder.getSalesOrderStatus() == LoadingStatusEnum.FAILED_DELIVERY) {
            salesOrder.setStatus(OrderStatusEnum.Failed);
        } else if (salesOrder.getSalesOrderStatus() == LoadingStatusEnum.DELIVERED) {
            boolean allDelivered = salesOrder.areAllItemsDelivered();
            if (allDelivered) {
                salesOrder.setStatus(OrderStatusEnum.Delivered);
            } else {
                salesOrder.setStatus(OrderStatusEnum.Partially_Delivered);
            }
        }
        salesOrdersRepository.save(salesOrder);
    }

    private boolean hasAnyItemsDelivered(SalesOrders salesOrder) {
        if (salesOrder.getSalesOrderItems() == null || salesOrder.getSalesOrderItems().isEmpty()) {
            return false;
        }

        return salesOrder.getSalesOrderItems().stream()
                .anyMatch(item -> item.getQuantityDelivered() > 0
                        || item.getStatus() == OrderItemStatus.Delivered
                        || item.getStatus() == OrderItemStatus.PartiallyDelivered);
    }

    private boolean shouldAutoFailOnAmendmentApproval(OrderAmendment orderAmendment, SalesOrders salesOrder) {
        if (hasAnyItemsDelivered(salesOrder) || hasAnyAmendmentItemsDelivered(orderAmendment)) {
            return false;
        }
        // cancelOrder uses amendOrderOrderCancellation which leaves rejectionsCode unset
        if (orderAmendment.getRejectionsCode() == null) {
            return true;
        }
        return isAmendmentCancellingEntireOrder(orderAmendment);
    }

    private boolean isAmendmentCancellingEntireOrder(OrderAmendment orderAmendment) {
        if (orderAmendment.getAmendmentProductItems() == null || orderAmendment.getAmendmentProductItems().isEmpty()) {
            return false;
        }

        return orderAmendment.getAmendmentProductItems().stream()
                .allMatch(this::isFullQuantityRemovedViaAmendment);
    }

    private boolean isFullQuantityRemovedViaAmendment(AmendmentProductItem item) {
        int delivered = item.getDeliveredQuantity() != null ? item.getDeliveredQuantity() : 0;
        int issued = item.getIssuedQuantity() != null ? item.getIssuedQuantity() : 0;
        int amendmentQty = item.getAmendmentQuantity() != null ? item.getAmendmentQuantity() : 0;
        return delivered == 0 && issued > 0 && amendmentQty == issued;
    }

    private boolean hasAnyAmendmentItemsDelivered(OrderAmendment orderAmendment) {
        if (orderAmendment.getAmendmentProductItems() == null || orderAmendment.getAmendmentProductItems().isEmpty()) {
            return false;
        }

        return orderAmendment.getAmendmentProductItems().stream()
                .anyMatch(item -> item.getDeliveredQuantity() != null && item.getDeliveredQuantity() > 0);
    }

    private void applyFailedDeliveryOnAmendmentApproval(SalesOrders salesOrder, String approvedBy, boolean failedOrder) {
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.FAILED_DELIVERY);
        salesOrder.setUpdateDate(LocalDateTime.now());
        salesOrder.setDeliveryDate(LocalDateTime.now());

        String description = String.format("Order marked as failed on amendment approval by %s for order %s",
                approvedBy,
                salesOrder.getSalesOrderNumber());

        eventsRepository.save(new Events(salesOrder.getPublicId().toString(),
                "Order Failed",
                LocalDateTime.now(),
                description,
                "Order Failed",
                null));

        updateOrderStatusAndDeliveredAmount(salesOrder);

        SalesOrders saved = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrder.getPublicId())
                .orElse(salesOrder);

        if (saved.isCreditApplied()) {
            try {
                bnplHelper.cancelCreditItemsForOrder(saved, null);
            } catch (Exception e) {
                log.error("Error cancelling credit items for order {}: {}", saved.getSalesOrderNumber(), e.getMessage());
            }
        }

        if (failedOrder) {
            Optional<RoutePlanning> routePlanningOptional = routePlanningRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(salesOrder.getRoutePublicId()));
            if (routePlanningOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Route  not found")
                        .userMessage("Sorry. Route  not found");
            }
            RoutePlanning routePlanning = routePlanningOptional.get();

            ProductRecon productRecon = productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(routePlanning.getRouteId()).orElse(null);

            if (productRecon == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Missing product recon")
                        .userMessage("Sorry. Missing product recon");
            }


            updateVanSales(routePlanning, salesOrder, productRecon, null);
        }
    }

    private void updateCreditAmountOnDelivery(SalesOrders salesOrder) {
        SalesOrders order = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrder.getPublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sales order with id " + salesOrder.getPublicId() + " not found")
                        .userMessage("Sorry. Sales order not found"));

        log.info("[DISCOUNT-TRACE] updateCreditAmountOnDelivery entry: order={} status={} totalAmountDelivered={} totalDiscount={} totalAmountPayable={} creditAmountBefore={}",
                order.getSalesOrderNumber(), order.getSalesOrderStatus(), order.getTotalAmountDelivered(),
                order.getTotalDiscount(), order.getTotalAmountPayable(), order.getCreditAmount());

        bnplHelper.createUpdateCreditItems(order, null);

        Optional<CreditLimitItem> creditLimitItemOptional = creditLimitItemRepository.findByOrderNumberAndStatusAndDeletedIsFalse(
                order.getSalesOrderNumber(),
                WalletItemStatus.ACTIVE
        );

        if (creditLimitItemOptional.isPresent()) {
            CreditLimitItem creditLimitItem = creditLimitItemOptional.get();
            order.setInterestAmount(creditLimitItem.getInterestAmount());
            order.setStaticDiscount(order.getTotalDiscount());
            salesOrdersRepository.save(order);

            creditLimitItem.setOrderDelivered(true);
            creditLimitItem.setDiscount(order.getTotalDiscount());
            creditLimitItemRepository.save(creditLimitItem);

            log.info("[DISCOUNT-TRACE] updateCreditAmountOnDelivery exit: order={} creditLimitItemAmount={} interestAmount={}",
                    order.getSalesOrderNumber(), creditLimitItem.getAmount(), creditLimitItem.getInterestAmount());
        } else {
            log.warn("[DISCOUNT-TRACE] updateCreditAmountOnDelivery: no ACTIVE CreditLimitItem found after credit update for order={}",
                    order.getSalesOrderNumber());
        }

    }


    private void sendCustomerDeliveryNotification(SalesOrders order, String deliveredBy) {

        SalesOrders saved = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(order.getPublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sales order with id " + order.getPublicId() + " not found")
                        .userMessage("Sorry. Sales order not found"));

        Locale swahiliLocale = new Locale("sw", "KE");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "EEE - MMM-yyyy, HH:mm",
                swahiliLocale
        );
        Set<SalesOrderItems> salesOrderItems = saved.getSalesOrderItems()
                .stream()
                .filter(salesOrderItems1 -> salesOrderItems1.getStatus() == OrderItemStatus.Delivered || salesOrderItems1.getStatus() == OrderItemStatus.PartiallyDelivered)
                .collect(Collectors.toSet());

        BigDecimal totalTaxPayable = salesOrderItems.stream()
                .map(SalesOrderItems::getTotalTaxPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPayable = salesOrderItems.stream()
                .map(SalesOrderItems::getTotalPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPayableWithTax = salesOrderItems.stream()
                .map(SalesOrderItems::getTotalPayableWithTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Customers customer = saved.getCustomer();
        String orderId = saved.getSalesOrderNumber();

        String message = "Dear " + customer.getName() + ",\n" +
                "Your order with ID " + orderId + " has been delivered.\n\n" +
                "Order Details:\n" +
                "Total Payable: " + totalPayable + "\n" +
                "Total Payable with Tax: " + totalPayableWithTax + "\n" +
                "Total Tax Payable: " + totalTaxPayable + "\n" +
                "\nDelivery By: " + deliveredBy + " " +
                "\nDelivery Date: " + LocalDateTime.now().format(formatter) + " " +
                "\nThank you for choosing us!\n " +
                "Itibari Tanzania\n";

        //itibariSmsService.send(customer.getPhoneNumber(), message);

    }

    private void checkVanSales(RoutePlanning routePlanning, SalesOrders saved, ProductRecon productRecon) {

        log.info("Checking if van sales are delivered");

        Optional<OrderAmendment> orderAmendmentOptional = orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(saved
                .getPublicId()
                .toString());

        if (orderAmendmentOptional.isEmpty()) {
            log.info("No order amendment found for sales order found");
            return;
        }

        OrderAmendment orderAmendment = orderAmendmentOptional.get();

        if (orderAmendment.getStatus() != AmendmentStatus.Approved) {
            log.info("Order amendment {} is not approved, skipping van sale check", orderAmendment.getPublicId());
            return;
        }

        DeliveryAgents deliveryAgent = saved.getDeliveryAgent();

        SalesOrders salesOrders = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(saved.getPublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sales order with id " + saved.getPublicId() + " not found")
                        .userMessage("Sorry. Sales order not found"));

        List<SalesOrderItems> salesOrderItems = salesOrders.getSalesOrderItems().stream()
                .filter(this::shouldConvertOrderItemToVanSale)
                .toList();

        log.info("Sales order items found: {}", salesOrderItems.size());

        if (!salesOrderItems.isEmpty() && productRecon != null) {
            log.info("Found {} sales order items for sales order {} and route {}",
                    salesOrderItems.size(),
                    salesOrders.getSalesOrderNumber(),
                    routePlanning.getRouteId());

            List<ProductItem> saveProductItems = new ArrayList<>();
            salesOrderItems.forEach(item -> addOrderItemToVanSales(routePlanning, salesOrders, deliveryAgent, productRecon, item)
                    .ifPresent(saveProductItems::add));

            productRecon.setUpdateDate(LocalDateTime.now());
            productReconRepository.save(productRecon);
            updatePreBookedOperationQuantities(routePlanning, salesOrders);
        } else {
            log.info("No sales order items found for sales order {} and route {}",
                    salesOrders.getSalesOrderNumber(),
                    routePlanning.getRouteId());
        }
    }

    @Deprecated
    @Override
    public void verifyCustomerLocation(UUID customerPublicId, String name) {

        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(name);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with phone number " + name + " not found")
                    .userMessage("Sorry. Delivery agent not found");
        }
        DeliveryAgents deliveryAgents = deliveryAgentOptional.get();

        Optional<Customers> customersOptional = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId);
        if (customersOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with id " + customerPublicId + " not found")
                    .userMessage("Sorry. Customer not found");
        }
        Customers customers = customersOptional.get();

        String otp = RandomStringUtils.randomNumeric(6);
        customers.setLocationVerificationOtp(otp);

        // itibariSmsService.send(customers.getPhoneNumber(), "Your Location verification code is " + otp);

        customers.setLocationCoordinatesVerified(false);
        customers.setUpdateDate(LocalDateTime.now());
        customersRepository.save(customers);

        String description = String.format("Location verification OTP sent to customer %s by delivery agent %s",
                customers.getName(),
                deliveryAgents.getFullName());
        eventsRepository.save(new Events(customers.getPublicId().toString(),
                "Customer Location Verification",
                LocalDateTime.now(),
                description,
                "Location Verification",
                null));
    }

    @Override
    public void verifyCustomerLocationWithOtp(UUID customerPublicId,
                                              VerifyCustomerLocationRequest verifyCustomerLocationRequest,
                                              String name) {

        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(name);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with phone number " + name + " not found")
                    .userMessage("Sorry. Delivery agent not found");
        }
        DeliveryAgents deliveryAgents = deliveryAgentOptional.get();

        Optional<Customers> customersOptional = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId);
        if (customersOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with id " + customerPublicId + " not found")
                    .userMessage("Sorry. Customer not found");
        }
        Customers customers = customersOptional.get();

        /*
        otp was removed because of charges
         */

//        if (customers.getLocationVerificationOtp() == null || !customers
//                .getLocationVerificationOtp()
//                .equalsIgnoreCase(verifyCustomerLocationRequest.getOtp())) {
//            throw new BadRequestRestApiException()
//                    .developerMessage("Invalid OTP")
//                    .userMessage("Sorry. Invalid OTP");
//        }

        customers.setLocationCoordinatesVerified(true);
        customers.setLocationVerificationOtp(null);
        customers.setUpdateDate(LocalDateTime.now());
        customers.setLongitude(verifyCustomerLocationRequest.getLongitude());
        customers.setLatitude(verifyCustomerLocationRequest.getLatitude());
        customersRepository.save(customers);

        String description = String.format("Location verified for customer %s by delivery agent %s",
                customers.getName(),
                deliveryAgents.getFullName());
        eventsRepository.save(new Events(customers.getPublicId().toString(),
                "Customer Location Verification",
                LocalDateTime.now(),
                description,
                "Location Verified",
                null));
    }

    @Override
    @Transactional
    public void cancelOrder(UUID salesOrderPublicId, CancelReasonRequest cancelReasonRequest, String name) {
//        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
//        if (!personOptional.isPresent()){
//            throw new BadRequestRestApiException()
//                    .developerMessage("User not found")
//                    .userMessage("Sorry, user not found");
//        }

        Person person = null;

        SalesOrders salesOrder = salesOrdersService.getSalesOrder(salesOrderPublicId);

        if (hasAnyItemsDelivered(salesOrder)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Cannot fail full order " + salesOrder.getSalesOrderNumber() + " when items were partially delivered")
                    .userMessage("Sorry. Use the amendment flow for partially delivered orders");
        }

        AmendOrderRequest amendOrderRequest = new AmendOrderRequest();
        amendOrderRequest.setDeliveryAgentPublicId(salesOrder.getDeliveryAgent().getPublicId());
        amendOrderRequest.setAmendAction(AmendAction.CancelAndRequestVanSale);
        amendOrderRequest.setRoutePublicId(UUID.fromString(salesOrder.getRoutePublicId()));
        amendOrderRequest.setSalesOrderPublicId(salesOrder.getPublicId());
        amendOrderRequest.setDescription(cancelReasonRequest.validReason());

        amendOrderOrderCancellation(salesOrder.getDeliveryAgent().getPublicId(), amendOrderRequest, person);

        RejectionReason rejectionReason = new RejectionReason();
        rejectionReason.setValidReason(cancelReasonRequest.validReason());
        rejectionReason.setSalesOrders(salesOrder);
//        personOptional.ifPresent(salesOrder::setCreatedBy);
        salesOrder.setUpdateDate(LocalDateTime.now());
        rejectionReasonRepository.save(rejectionReason);

        String description = String.format("Order cancellation requested by %s for order %s with reason: %s",
                name,
                salesOrder.getSalesOrderNumber(),
                rejectionReason.getValidReason());

        eventsRepository.save(new Events(salesOrder.getPublicId().toString(),
                "Order Cancellation Requested",
                LocalDateTime.now(),
                description,
                "Order Cancellation Requested",
                null));

        salesOrdersRepository.save(salesOrder);
    }


    private void updateProductItemsDiscounts(RoutePlanning routePlanning) {
        if (routePlanning == null) {
            log.info("Route planning is null, skipping product items discount update");
            return;
        }

        Optional<ProductRecon> optionalProductRecon = productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(
                routePlanning.getDeliveryAgentId(),
                routePlanning.getRouteId());

        if (optionalProductRecon.isEmpty()) {
            log.info("No product recon found for delivery agent id {} and delivery id {}, skipping product items discount update",
                    routePlanning.getDeliveryAgentId(),
                    routePlanning.getRouteId());
            return;
        }

        ProductRecon productRecon = optionalProductRecon.get();

        List<SalesOrderItems> orderItems = routePlanning.getSalesOrders()
                .stream()
                .filter(s -> s.getSalesOrderStatus() == LoadingStatusEnum.DELIVERED)
                .flatMap(salesOrder -> salesOrder.getSalesOrderItems().stream())
                .filter(SalesOrderItems::isHasDiscount)
                .toList();


        if (!orderItems.isEmpty()) {
            for (SalesOrderItems item : orderItems) {
                log.info("Updating discount for order item: {}", item.getPublicId());

                Optional<ProductItem> productItemOptional = productItemRepository.findByProductReconAndProductPublicIdAndDeletedIsFalse(
                        productRecon, item.getProduct().getPublicId());


                if (productItemOptional.isPresent()) {
                    ProductItem productItem = productItemOptional.get();
                    productItem.setConfirmedDiscount(item.getDiscount());

                    productItemRepository.save(productItem);
                    log.info("Discount updated for product item: {}", productItem.getPublicId());
                }

            }
        }

        // update recon

        Optional<Reconciliation> reconciliationOptional = reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(routePlanning.getPublicId().toString());

        if (reconciliationOptional.isPresent()) {
            Reconciliation reconciliation = reconciliationOptional.get();

            BigDecimal creditTotal = BigDecimal.ZERO;

            for (SalesOrders order : routePlanning.getSalesOrders()) {
                if (order.isCreditApplied()) {
                    Optional<CreditLimitItem> creditLimitItemOptional = creditLimitItemRepository.findByOrderNumberAndStatusAndDeletedIsFalse(
                            order.getSalesOrderNumber(),
                            WalletItemStatus.ACTIVE
                    );

                    if (creditLimitItemOptional.isPresent()) {
                        CreditLimitItem creditLimitItem = creditLimitItemOptional.get();
                        creditTotal = creditTotal.add(creditLimitItem.getAmount());
                        log.info("[DISCOUNT-TRACE] recon creditTotal component: order={} creditLimitItemAmount(net)={} orderDiscount={} orderDeliveredGross={}",
                                order.getSalesOrderNumber(), creditLimitItem.getAmount(), order.getTotalDiscount(), order.getTotalAmountDelivered());
                    } else {
                        log.warn("[DISCOUNT-TRACE] recon creditTotal: creditApplied order={} has NO ACTIVE CreditLimitItem — excluded from recon creditAmount",
                                order.getSalesOrderNumber());
                    }
                }
            }
            reconciliation.setCreditAmount(creditTotal);
            reconciliation.setUpdateDate(LocalDateTime.now());
            reconciliationRepository.save(reconciliation);
            log.info("[DISCOUNT-TRACE] recon {} creditAmount set to {} (route={})",
                    reconciliation.getPublicId(), creditTotal, routePlanning.getRouteId());
        }
    }


    public void amendOrderOrderCancellation(UUID deliveryAgentPublicId, AmendOrderRequest amendOrderRequest,
                                            Person person) {

        Optional<DeliveryAgents> optionalDeliveryAgents = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(deliveryAgentPublicId);

        if (optionalDeliveryAgents.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with id " + deliveryAgentPublicId + " not found")
                    .userMessage("Sorry. Delivery agent with id " + deliveryAgentPublicId + " not found");
        }

        if (amendOrderRequest.getAmendAction() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Amend action is required")
                    .userMessage("Sorry. Amend action is required");
        }

        DeliveryAgents deliveryAgent = optionalDeliveryAgents.get();

        Optional<RoutePlanning> routePlanningOptional = routePlanningRepository.findByPublicIdAndDeletedIsFalse(amendOrderRequest.getRoutePublicId());
        if (routePlanningOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route with id " + amendOrderRequest.getRoutePublicId() + " not found")
                    .userMessage("Sorry. Route  not found");
        }
        RoutePlanning routePlanning = routePlanningOptional.get();

        ProductRecon productRecon = productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(routePlanning.getRouteId()).orElse(null);

        if (productRecon == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing product recon")
                    .userMessage("Sorry. Missing product recon");
        }


        OrderAmendment orderAmendment = new OrderAmendment();

        Optional<SalesOrders> salesOrderOptional = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(amendOrderRequest.getSalesOrderPublicId());

        if (salesOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order with id " + amendOrderRequest.getSalesOrderPublicId() + " not found")
                    .userMessage("Sorry. Sales order not found");
        }

        SalesOrders salesOrder = salesOrderOptional.get();

        if (amendOrderRequest.getAmendAction() != AmendAction.RequestTransferVehicle) {

            if (amendOrderRequest.getSalesOrderPublicId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales order public id is required")
                        .userMessage("Sorry. Sales order public id is required");
            }

            salesOrder.setAwaitingApproval(true);
            salesOrder.setOrderAmendStatus(OrderAmendStatus.Pending);
            orderAmendment.setSalesOrder(salesOrder);
            salesOrdersRepository.save(salesOrder);
        }
        orderAmendment.setDescription(amendOrderRequest.getDescription());
        orderAmendment.setStatus(AmendmentStatus.Pending);
        orderAmendment.setRouteId(routePlanning.getRouteId());
        orderAmendment.setRoutePublicId(routePlanning.getPublicId().toString());
        orderAmendment.setAmendAction(amendOrderRequest.getAmendAction());
        orderAmendment.setDeliveryAgent(deliveryAgent);
        orderAmendment.setRejectionsCode(null);
        orderAmendment.setCreationTime(LocalDateTime.now());
        orderAmendment.setSalesOrderPublicId(amendOrderRequest.getSalesOrderPublicId().toString());
        orderAmendment.setFailedOrder(true);
        OrderAmendment orderAmendment1 = orderAmendmentRepository.save(orderAmendment);

        if (!amendOrderRequest.getItems().isEmpty()) {
            Set<SalesOrderItems> orderItems = salesOrder.getSalesOrderItems();
            List<AmendmentProductItem> amendmentProductItems = new ArrayList<>();

            for (SalesOrderItems salesOrderItems : orderItems) {
                for (ItemRequest itemRequest : amendOrderRequest.getItems()) {
                    if (salesOrderItems.getPublicId().equals(itemRequest.getProductItemPublicId())) {
                        AmendmentProductItem amendmentProductItem = new AmendmentProductItem();
                        amendmentProductItem.setOrderAmendment(orderAmendment1);
                        amendmentProductItem.setAmendmentQuantity(salesOrderItems.getQuantity());
                        amendmentProductItem.setProductCode(salesOrderItems.getProduct().getSku());
                        amendmentProductItem.setProductName(salesOrderItems.getProduct().getProductName());
                        amendmentProductItem.setIssuedQuantity(salesOrderItems.getQuantity());
                        amendmentProductItem.setPricePerUnit(salesOrderItems.getPricePerUnit());

                        AmendmentProductItem amendmentProductItem1 = orderAmendmentItemRepository.save(amendmentProductItem);
                        amendmentProductItems.add(amendmentProductItem1);
                    }
                }
            }

            orderAmendment1.setAmendmentProductItems(amendmentProductItems);
        } else {
            Set<SalesOrderItems> orderItems = salesOrder.getSalesOrderItems();
            List<AmendmentProductItem> amendmentProductItems = new ArrayList<>();

            for (SalesOrderItems salesOrderItems : orderItems) {
                AmendmentProductItem amendmentProductItem = new AmendmentProductItem();
                amendmentProductItem.setOrderAmendment(orderAmendment1);
                amendmentProductItem.setAmendmentQuantity(salesOrderItems.getQuantity());
                amendmentProductItem.setProductCode(salesOrderItems.getProduct().getSku());
                amendmentProductItem.setProductName(salesOrderItems.getProduct().getProductName());
                amendmentProductItem.setIssuedQuantity(salesOrderItems.getQuantity());
                amendmentProductItem.setDeliveredQuantity(0);
                amendmentProductItem.setPricePerUnit(salesOrderItems.getPricePerUnit());

                AmendmentProductItem amendmentProductItem1 = orderAmendmentItemRepository.save(amendmentProductItem);
                amendmentProductItems.add(amendmentProductItem1);
            }
        }
        OrderAmendment savedOrderAmendment = orderAmendmentRepository.save(orderAmendment1);

        String title = "Order Amendment - fail order";
        String message = String.format("Order amendment request has been raised by %s %s for order %s with action %s. Description: %s",
                deliveryAgent.getFirstName(),
                deliveryAgent.getLastName(),
                savedOrderAmendment.getSalesOrder().getSalesOrderNumber(),
                savedOrderAmendment.getAmendAction().name(),
                savedOrderAmendment.getDescription());

        notificationHelper.createNotificationHelper(
                title,
                message,
                "ROLE_ADMIN",
                savedOrderAmendment.getPublicId(),
                "ORDER_AMENDMENT",
                "APPROVE_DELIVERY_AGENT");

    }

    @Override
    public String sendOtpToCustomer(UUID orderPublicId, String name) {
        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(name);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with phone number " + name + " not found")
                    .userMessage("Sorry. Delivery agent not found");
        }
        DeliveryAgents deliveryAgents = deliveryAgentOptional.get();

        Optional<SalesOrders> salesOrderOptional = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(orderPublicId);
        if (salesOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order with id " + orderPublicId + " not found")
                    .userMessage("Sorry. Sales order not found");
        }
        SalesOrders salesOrders = salesOrderOptional.get();

        if (!salesOrders.isCreditApplied()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order with id " + orderPublicId + " is not a credit order")
                    .userMessage("Sorry. Sales order is not a credit order");
        }

        Customers customers = salesOrders.getCustomer();

        String otp = RandomStringUtils.randomNumeric(6);

        itibariSmsService.send(customers.getPhoneNumber(), "Your Order verification code is " + otp);

        customers.setUpdateDate(LocalDateTime.now());
        customersRepository.save(customers);

        String description = String.format("Credit verification OTP sent to customer %s by delivery agent %s for order %s",
                customers.getName(),
                deliveryAgents.getFullName(),
                salesOrders.getSalesOrderNumber());
        eventsRepository.save(new Events(customers.getPublicId().toString(),
                "Customer Order Verification",
                LocalDateTime.now(),
                description,
                "Order Verification",
                null));

        salesOrders.setCreditOneTimePassword(otp);
        salesOrders.setUpdateDate(LocalDateTime.now());
        salesOrdersRepository.save(salesOrders);

        return otp;
    }


    @Override
    public void generateTraCustomerReceipt(SalesOrders salesOrder) {

        if (!traSwitchService.traSwitchOn()) {
            log.info("TRA switch is off. Skipping TRA customer receipt generation for sales order no : {}", salesOrder.getSalesOrderNumber());
            return;
        }

        log.info("Generating TRA customer receipt for sales order no : {}", salesOrder.getSalesOrderNumber());
        TRARequest request = traUtilHelper.convertToTraRequest(salesOrder);

        Optional<TRAResponse> responseOptional =
                traSubmissionHelper.submitReceipt(request, salesOrder.getSalesOrderNumber());

        if (responseOptional.isEmpty()) {
            return;
        }

        log.info("Generating and sending TRA customer receipt for sales order: {}", salesOrder.getSalesOrderNumber());
        traUtilHelper.generateTraCustomerReceiptAndSend(request, responseOptional.get(), salesOrder);
        log.info("TRA customer receipt generated and sent for sales order: {}", salesOrder.getSalesOrderNumber());

    }

    @Override
    public void generateTraReceiptSchedular() {

        if (!traSwitchService.traSwitchOn()) {
            return;
        }
        List<SalesOrders> salesOrdersList = salesOrdersRepository.findAllByReceiptFailedIsTrueAndDeletedIsFalse();

        for (SalesOrders salesOrder : salesOrdersList) {

            try {

                if (salesOrder.getCustomer().getTin() != null && !salesOrder.getCustomer().getTin().isEmpty()) {
                    if (!customerCheckerHelper.validateCustomerTinNumber(salesOrder.getCustomer())) {
                        return;
                    }

                }
                this.generateTraCustomerReceipt(salesOrder);


            } catch (Exception e) {
                log.error("Error updating credit amount on delivery: {}", e.getMessage());
            }

//            try {
//                if (customerCheckerHelper.validateCustomerTinNumber(salesOrder.getCustomer())) {
//                    this.generateTraCustomerReceipt(salesOrder);
//                }
//            } catch (Exception e) {
//                log.error("Error generating TRA receipt for sales order {}: {}", salesOrder.getSalesOrderNumber(), e.getMessage());
//            }
        }


    }

    @Override
    public void requestLocationApproval(LocationApprovalRequest locationApprovalRequest, String name) {
        Optional<DeliveryAgents> deliveryAgentOptional = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(name);

        if (deliveryAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent with phone number " + name + " not found")
                    .userMessage("Sorry. Delivery agent not found");
        }
        DeliveryAgents deliveryAgents = deliveryAgentOptional.get();

        LocationApproval locationApproval = new LocationApproval();

        Optional<SalesOrders> optionalSalesOrders = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(locationApprovalRequest.getOrderPublicId());

        if (optionalSalesOrders.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order with id " + locationApprovalRequest.getOrderPublicId() + " not found")
                    .userMessage("Sorry. Sales order not found");
        }

        if (locationApprovalRequest.getRejectionsCodePublicId() != null) {
            RejectionsCode rejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(locationApprovalRequest.getRejectionsCodePublicId())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Rejection code with id " + locationApprovalRequest.getRejectionsCodePublicId() + " not found")
                            .userMessage("Sorry. A reason is not found!"));

            String comment = locationApprovalRequest.getComment() != null ? locationApprovalRequest.getComment() + "( " + rejectionsCode.getDescription() + " )" : rejectionsCode.getDescription();
            locationApproval.setComment(comment);
        }

        SalesOrders salesOrders = optionalSalesOrders.get();

        RoutePlanning routePlanning = routePlanningRepository.findByPublicId(locationApprovalRequest.getRoutePublicId());

        if (routePlanning == null) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry. Route planning not found")
                    .developerMessage("Route planning with id " + salesOrders.getRoutePublicId() + " not found");
        }


        locationApproval.setDeliveryAgent(deliveryAgents.getFullName());
        locationApproval.setLatitude(locationApprovalRequest.getLatitude());
        locationApproval.setLongitude(locationApprovalRequest.getLongitude());
        locationApproval.setComment(locationApprovalRequest.getComment());
        locationApproval.setCustomerName(salesOrders.getCustomer().getName());
        locationApproval.setCustomerPhone(salesOrders.getCustomer().getPhoneNumber());
        locationApproval.setCustomerPublicId(salesOrders.getCustomer().getPublicId().toString());
        locationApproval.setStatus(LocationApprovalStatus.PENDING);
        locationApproval.setRoutePublicId(routePlanning.getPublicId().toString());
        locationApproval.setPreviousLatitude(salesOrders.getCustomer().getLatitude());
        locationApproval.setPreviousLongitude(salesOrders.getCustomer().getLongitude());
        locationApproval.setOrderPublicId(salesOrders.getPublicId().toString());
        locationApproval.setNumberOfAttempts(salesOrders.getDaLocationApprovalRequestAttempts() + 1);


        locationApprovalRepository.save(locationApproval);

        routePlanning.setLocked(true);
        routePlanningRepository.save(routePlanning);

        salesOrders.setLocked(true);
        salesOrders.setDaLocationApprovalRequestAttempts(salesOrders.getDaLocationApprovalRequestAttempts() + 1);
        salesOrdersRepository.save(salesOrders);

    }

    @Override
    public Page<LocationApproval> getLocationApprovals(String searchParam, List<LocationApprovalStatus> statuses, String deliveryAgent, String customer, String from, String to, Pageable pageable) {
        Specification<LocationApproval> spec = Specification
                .where(LocationApprovalSpecification.searchParamMatches(searchParam))
                .and(LocationApprovalSpecification.hasStatuses(statuses))
                .and(LocationApprovalSpecification.hasDeliveryAgent(deliveryAgent))
                .and(LocationApprovalSpecification.hasCustomer(customer))
                .and(LocationApprovalSpecification.isBetweenDates(from, to))
                .and(LocationApprovalSpecification.isNotDeleted());

        return locationApprovalRepository.findAll(spec, pageable);
    }

    @Override
    public List<DeliveryAgents> getDeliveryAgentMiniData(String searchParam) {
        Sort sortByNameAsc = Sort.by(Sort.Direction.ASC, "firstName");

        if (searchParam != null && !searchParam.trim().isEmpty()) {
            return deliveryAgentsRepository
                    .searchByName(searchParam, sortByNameAsc);
        } else {
            return deliveryAgentsRepository
                    .findDeliveryAgentsByDeletedIsFalse(sortByNameAsc);
        }
    }

    @Override
    public void approveOrRejectLocationApproval(UUID locationApprovalId, boolean approve, UUID rejectionCodePublicId, String name) {
        Person loggedInUser = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("User with email " + name + " not found")
                        .userMessage("Sorry. User not found"));

        LocationApproval locationApproval = locationApprovalRepository.findByPublicIdAndDeletedIsFalse(locationApprovalId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Location approval with id " + locationApprovalId + " not found")
                        .userMessage("Sorry. Location approval not found"));


        /*
          approval and rejection requires a reason.
         */
        if (rejectionCodePublicId == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Rejection code public id is required for rejection")
                    .userMessage("Sorry. A reason is approved");
        }
        RejectionsCode rejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(rejectionCodePublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Rejection code with id " + rejectionCodePublicId + " not found")
                        .userMessage("Sorry. A reason is not found!"));

        if (approve) {

//            if (rejectionsCode.getModule() != Module.LocationApprovalCode) {
//                throw new BadRequestRestApiException()
//                        .developerMessage("Rejection code with id " + rejectionCodePublicId + " is not valid for location approval approval")
//                        .userMessage("Sorry. Invalid reason code");
//            }
            locationApproval.setStatus(LocationApprovalStatus.APPROVED);

        } else {


//            if (rejectionsCode.getModule() != Module.LocationRejectionCode) {
//                throw new BadRequestRestApiException()
//                        .developerMessage("Rejection code with id " + rejectionCodePublicId + " is not valid for location approval rejection")
//                        .userMessage("Sorry. Invalid rejection code");
//            }
            locationApproval.setStatus(LocationApprovalStatus.REJECTED);
        }

        RoutePlanning routePlanning = locationApproval.getRoutePublicId() != null ? routePlanningRepository.findByPublicId(UUID.fromString(locationApproval.getRoutePublicId())) : null;
        if (routePlanning != null) {
            routePlanning.setLocked(false);
            routePlanningRepository.save(routePlanning);
        }

        // order

        SalesOrders order = locationApproval.getOrderPublicId() != null ? salesOrdersRepository.findByPublicId(UUID.fromString(locationApproval.getOrderPublicId())) : null;

        if (order != null) {
            order.setLocked(false);
            salesOrdersRepository.save(order);


            // update customer lat and long

            if (approve) {
                Customers customer = order.getCustomer();
                customer.setLongitude(locationApproval.getLongitude());
                customer.setLatitude(locationApproval.getLatitude());
                customersRepository.save(customer);
            }
        }

        locationApproval.setUpdatedBy(loggedInUser);
        locationApproval.setUpdateDate(LocalDateTime.now());
        locationApproval.setComments(rejectionsCode.getDescription());
        locationApprovalRepository.save(locationApproval);
    }

    @Override
    public void unlockLocationApprovalRequest(UUID locationApprovalPublicId, String name) {
        Person loggedInUser = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("User with email " + name + " not found")
                        .userMessage("Sorry. User not found"));

        LocationApproval locationApproval = locationApprovalRepository.findByPublicIdAndDeletedIsFalse(locationApprovalPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Location approval with id " + locationApprovalPublicId + " not found")
                        .userMessage("Sorry. Location approval not found"));

        RoutePlanning routePlanning = routePlanningRepository.findByPublicId(UUID.fromString(locationApproval.getRoutePublicId()));
        if (routePlanning != null) {
            routePlanning.setLocked(false);
            routePlanningRepository.save(routePlanning);
        }

        // order

        SalesOrders order = salesOrdersRepository.findByPublicId(UUID.fromString(locationApproval.getOrderPublicId()));

        if (order != null) {
            order.setLocked(false);
            salesOrdersRepository.save(order);
        }

        locationApproval.setStatus(LocationApprovalStatus.APPROVED);
        locationApproval.setUpdatedBy(loggedInUser);
        locationApproval.setUpdateDate(LocalDateTime.now());
        locationApprovalRepository.save(locationApproval);
    }


    @Override
    @Transactional
    public void sellVanSaleItem(SellVanSaleRequest vanSaleRequest, String name) {

        Customers customers = validateCustomer(vanSaleRequest.getCustomerPublicId());
        log.info("====selling van sale for===== {}", customers.getFullName());

        RoutePlanning routePlanning = validateRoutePlanning(vanSaleRequest.getRoutePublicId());

        List<ProductItem> productItems = validateProductItems(vanSaleRequest.getItems(), routePlanning, vanSaleRequest.getRequestSpecialDiscount());

        try {
            if (vanSaleRequest.getRequestSpecialDiscount()) {
                // not working now..
                vanSalesHelper.createSpecialDiscountRequest(customers, routePlanning, vanSaleRequest.getItems(), productItems);
            } else {
                SalesOrders vanSalesOrder = vanSalesHelper.createVanSalesOrder(customers, routePlanning, vanSaleRequest.getItems(), productItems);
                routePlanning.addSalesOrder(vanSalesOrder);
                routePlanningRepository.save(routePlanning);
                applyVanSalesDiscounts(productItems, vanSalesOrder);
                //   this.updateProductItemsVanSales(productItems, vanSaleRequest.getItems());

                sellVansalesItems(vanSaleRequest.getItems(), routePlanning, vanSalesOrder);
            }

        } catch (Exception e) {
            log.error("Error validating van sale request: {}", e.getMessage());
            throw new BadRequestRestApiException()
                    .developerMessage("Error processing van sale request: " + e.getMessage())
                    .userMessage("Sorry. Error processing van sale");
        }

    }

    @Transactional
    @Override
    public void amendFullDeliveredOrder(UUID salesOrderPublicId, AmendOrderItemRequest amendOrderItemRequest, Principal principal) {
        Optional<SalesOrders> salesOrderOptional = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrderPublicId);
        if (salesOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order not found")
                    .userMessage("Sales order not found");
        }

        CustomerReturn customerReturnWrapper = createCustomerReturnWrapper(salesOrderOptional.get(), amendOrderItemRequest.itemList().size(), principal);
        createCustomerReturnWrapperItems(customerReturnWrapper, amendOrderItemRequest);

        salesOrderOptional.get().setAmended(true);
        salesOrdersRepository.save(salesOrderOptional.get());
//        createSalesOrderAmendment(salesOrderOptional.get(), amendOrderItemRequest, principal);
    }

    @Override
    public void approveCustomerReturn(UUID customerReturnPublicId, ReturnApprovalType returnApprovalType, Principal principal) {
        Person updatedBy = validatePerson(principal.getName());
        CustomerReturn customerReturn = validateCustomerReturn(customerReturnPublicId);
        SalesOrders salesOrder = customerReturn.getSalesOrder();
        DeliveryAgents deliveryAgent = getDeliveryAgent(salesOrder.getDeliveryAgent().getPublicId());

        RoutePlanning routePlanning = getRoute(salesOrder, deliveryAgent.getPublicId());

        ProductRecon productRecon = getProductRecon(routePlanning.getRouteId());

        if (returnApprovalType == ReturnApprovalType.Approve) {

            if (customerReturn.getStatus() == CustomerReturnStatus.Approved) {
                throw new BadRequestRestApiException()
                        .developerMessage("Return already Approved")
                        .userMessage("Sorry. Return already Approved");
            }

            List<CustomerReturnItem> customerReturnItems = customerReturn.getCustomerReturnItems();

            for (CustomerReturnItem customerReturnItem : customerReturnItems) {
                for (ProductItem productItem : productRecon.getProductItems()) {
                    if (productItem.getProductCode().equals(customerReturnItem.getSku())) {
                        int deliveredQuantity = (int) productItem.getQuantityPicked() - customerReturnItem.getReturnedQuantity();
                        updateProductItemSoldQuantity(productItem, deliveredQuantity, customerReturnItem.getReturnedQuantity());
                    }
                }
            }

            customerReturn.setUpdatedBy(updatedBy);
            customerReturn.setStatus(CustomerReturnStatus.Approved);
            CustomerReturn savedCustomerReturn = customerReturnRepository.save(customerReturn);

            createSalesOrderAmendment(savedCustomerReturn);

            updateStock(savedCustomerReturn, productRecon);

            for (CustomerReturnItem customerReturnItem : savedCustomerReturn.getCustomerReturnItems()) {
                liveStockHelper.updateLiveStockValue(UUID.fromString(productRecon.getWarehousePublicId()),
                        customerReturnItem.getProductPublicId(),
                        (int) customerReturnItem.getReturnedQuantity(),
                        ProductUpdateType.CUSTOMER_RETURN,
                        LocalDate.now());
            }
        }
    }

    private void createAmendmentYaCustomerReturn(CustomerReturn savedCustomerReturn) {

    }

    private void updateStock(CustomerReturn savedCustomerReturn, ProductRecon productRecon) {

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(productRecon.getWarehousePublicId())).orElse(null);
        Inventory inventory = getInventory(warehouse);
        for (CustomerReturnItem customerReturnItem : savedCustomerReturn.getCustomerReturnItems()) {
            Product product = productRepository.findByPublicId(customerReturnItem.getProductPublicId());
            if (product != null) {
                InventoryProducts inventoryProduct = inventoryProductsRepository.findByInventoryAndProductAndDeletedIsFalse(inventory, product).orElse(null);

                if (inventoryProduct == null) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Inventory product not found")
                            .userMessage("Sorry. Inventory product not found");
                }

                Integer returnedQuantity = customerReturnItem.getReturnedQuantity();

                inventoryProduct.setAvailableStock(inventoryProduct.getAvailableStock() + returnedQuantity);

                inventoryProduct.getProduct().setAvailableStock(inventoryProduct.getProduct().getAvailableStock() + returnedQuantity);

                Product saveProduct = productRepository.save(inventoryProduct.getProduct());

                inventoryProductsRepository.save(inventoryProduct);

                updateLiveStockReturnFromQuarantine(warehouse.getPublicId(), saveProduct.getPublicId(), returnedQuantity);

                productStockHelper.updateProductStock(saveProduct, warehouse, (long) returnedQuantity, StockMovementTransactionType.CUSTOMER_RETURN);

            }
        }
    }

    private void updateLiveStockReturnFromQuarantine(UUID warehousePublicId, UUID productPublicId, int quantity) {
        liveStockHelper.updateLiveStockValue(warehousePublicId,
                productPublicId,
                quantity,
                ProductUpdateType.SOLD_STOCK_MINUS_AMENDED,
                LocalDate.now());
    }

    private Inventory getInventory(Warehouse warehouse) {
        Optional<Inventory> optionalInventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse);
        if (optionalInventory.isPresent()) {
            return optionalInventory.get();
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Inventory not found")
                    .userMessage("Sorry. Inventory not found");
        }
    }

    private void createCustomerReturnWrapperItems(CustomerReturn customerReturnWrapper, AmendOrderItemRequest amendOrderItemRequest) {
        List<CustomerReturnItem> customerReturnItemList = new ArrayList<>();

        BigDecimal totalValue = BigDecimal.ZERO;

        for (AmendOrderItemRequest.Item item : amendOrderItemRequest.itemList()) {
            SalesOrderItems salesOrderItem = salesOrderItemsRepository
                    .findByPublicIdAndDeletedIsFalse(item.salesOrderItemPublicId())
                    .orElse(null);

            CustomerReturnItem customerReturnItem = customerReturnItemRepository
                    .findByProductPublicIdAndOrderItemPublicId(salesOrderItem.getProduct().getPublicId(), item.salesOrderItemPublicId())
                    .orElse(null);

            if (customerReturnItem != null) {
                customerReturnItem.setReturnedQuantity(item.returnedQuantity());
            } else {
                customerReturnItem = new CustomerReturnItem();

                if (salesOrderItem != null) {
                    customerReturnItem.setProductName(salesOrderItem.getProduct().getProductName());
                    customerReturnItem.setSku(salesOrderItem.getProduct().getSku());
                    customerReturnItem.setProductPublicId(salesOrderItem.getProduct().getPublicId());
                    customerReturnItem.setOrderItemPublicId(salesOrderItem.getPublicId());
                    customerReturnItem.setOrderQuantity(salesOrderItem.getQuantity());
                    customerReturnItem.setDeliveredQuantity(salesOrderItem.getQuantityDelivered());
                    customerReturnItem.setUnitPrice(salesOrderItem.getPricePerUnit());

                    BigDecimal result = salesOrderItem.getPricePerUnit().multiply(BigDecimal.valueOf(item.returnedQuantity()));
                    totalValue = totalValue.add(result);
                } else {
                    customerReturnItem.setOrderQuantity(0);
                    customerReturnItem.setDeliveredQuantity(0);
                    customerReturnItem.setUnitPrice(BigDecimal.ZERO);
                }

                customerReturnItem.setReturnedQuantity(item.returnedQuantity());

                RejectionsCode rejectionsCode = rejectionCodeRepository
                        .findRejectionsCodeByPublicIdAndDeletedIsFalse(item.reasonCodePublicId())
                        .orElseThrow(() -> new BadRequestRestApiException()
                                .developerMessage("Reason code not found")
                                .userMessage("Sorry, reason code not found"));

                customerReturnItem.setRejectionsCode(rejectionsCode);
            }

            CustomerReturnItem saved = customerReturnItemRepository.save(customerReturnItem);
            customerReturnItemList.add(saved);
        }

        customerReturnWrapper.getCustomerReturnItems().addAll(customerReturnItemList);

        customerReturnWrapper.setReturnedValue(totalValue);
        customerReturnRepository.save(customerReturnWrapper);

//        for (CustomerReturnItem customerReturnItem : customerReturnItemList) {
//            stockMovementHelper.recordStockMovement(
//                    customerReturnItem..getProduct(),
//                    item.getQuantitySold(),
//                    StockMovementTransactionType.DELIVERY,
//                    TransactionBoundType.RECORD,
//                    "Delivery agent " + deliveryAgent.getFullName(),
//                    salesOrders.getWarehouse()
//            );
//        }


    }


    private CustomerReturn createCustomerReturnWrapper(SalesOrders salesOrder, Integer productSize, Principal principal) {
        Person updatedBy = validatePerson(principal.getName());

        isOrderAmendmentPresent(salesOrder);

        DeliveryAgents deliveryAgent = getDeliveryAgent(salesOrder.getDeliveryAgent().getPublicId());

        CustomerReturn customerReturn = new CustomerReturn();
        customerReturn.setReturnId("CR" + counterService.getNextCounter(CounterType.CUSTOMER_RETURN));
        customerReturn.setDeliveryAgent(deliveryAgent);
        customerReturn.setSalesOrder(salesOrder);
        customerReturn.setCreatedBy(updatedBy);
        customerReturn.setProductSize(productSize);
        customerReturn.setReturnedValue(BigDecimal.ZERO);
        customerReturn.setStatus(CustomerReturnStatus.Pending);
        return customerReturnRepository.save(customerReturn);
    }

    private void updateProductItemSoldQuantity(ProductItem productItem, Integer deliveredQuantity, Integer returnedQuantity) {
        productItem.setQuantitySold(deliveredQuantity);
        productItem.setQuantityReturned(returnedQuantity);
        productItemRepository.save(productItem);
    }

    private boolean validateQuantity(int quantityDelivered, Integer returnedQuantity) {
        if (quantityDelivered < returnedQuantity) {
            throw new BadRequestRestApiException()
                    .developerMessage("Returned quantity is more than quantity delivered")
                    .userMessage("Sorry. Returned quantity is more than quantity delivered");
        }
        return true;
    }

    private RejectionsCode getRejectionsCode(@NotNull UUID reasonCodeId) {
        Optional<RejectionsCode> optionalRejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(reasonCodeId);

        if (optionalRejectionsCode.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Rejection code not found")
                    .userMessage("Sorry. Rejection code not found");
        }
        return optionalRejectionsCode.get();
    }

    private ProductRecon getProductRecon(String routeId) {
        ProductRecon productRecon = productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(routeId).orElse(null);

        if (productRecon == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing product recon")
                    .userMessage("Sorry. Missing product recon");
        }
        return productRecon;
    }

    private RoutePlanning getRoute(SalesOrders salesOrder, UUID deliveryAgentPublicId) {
        Optional<RoutePlanning> routePlanningOpt = routePlanningRepository
                .findByDeliveryAgentIdIgnoreCaseAndDeletedIsFalseAndSalesOrdersIn(
                        deliveryAgentPublicId.toString(),
                        Collections.singletonList(salesOrder)
                );
        RoutePlanning routePlanning = routePlanningOpt.orElse(null);
        if (routePlanning == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route not found")
                    .userMessage("Sorry. Route not found");
        }
        return routePlanning;
    }

    private void updateProductItemsVanSales(List<ProductItem> productItems, @NotEmpty List<VanItemRequest> items) {
        for (ProductItem productItem : productItems) {
            VanItemRequest itemRequest = items.stream()
                    .filter(item -> item.getProductItemPublicId().equals(productItem.getPublicId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Product item with id " + productItem.getPublicId() + " not found in request")
                            .userMessage("Sorry. Product item not found"));

            int newVanSaleQuantity = Math.toIntExact(productItem.getVanSaleQuantity() - itemRequest.getQuantitySold());
            if (newVanSaleQuantity < 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Not enough van sale quantity for product item with id " + productItem.getPublicId())
                        .userMessage(String.format("Sorry. Not enough van sale quantity for product  %s. Available quantity: %d", productItem.getProductName(), productItem.getVanSaleQuantity()));
            }
            productItem.setVanSaleQuantity(newVanSaleQuantity);
            productItemRepository.save(productItem);
        }
    }

    private void applyVanSalesDiscounts(List<ProductItem> productItems, SalesOrders vanSalesOrder) {
        Map<UUID, BigDecimal> discountsByProduct = vanSalesOrder.getSalesOrderItems().stream()
                .collect(Collectors.toMap(
                        item -> item.getProduct().getPublicId(),
                        item -> Optional.ofNullable(item.getDiscount()).orElse(BigDecimal.ZERO),
                        BigDecimal::add));

        productItems.forEach(productItem -> {
            BigDecimal volumeDiscount = discountsByProduct.getOrDefault(productItem.getProductPublicId(), BigDecimal.ZERO);
            productItem.setDiscount(Optional.ofNullable(productItem.getDiscount()).orElse(BigDecimal.ZERO).add(volumeDiscount));
        });
    }

    private List<ProductItem> validateProductItems(@NotEmpty List<VanItemRequest> items, RoutePlanning routePlanning, Boolean requestSpecialDiscount) {
        ProductRecon productRecon = productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(routePlanning.getRouteId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Product recon not found for route " + routePlanning.getRouteId())
                        .userMessage("Sorry. Product recon not found for this route"));

        List<ProductItem> productItems = items.stream()
                .map(item -> productItemRepository.findByProductReconAndPublicIdAndDeletedIsFalse(productRecon, item.getProductItemPublicId())
                        .orElseThrow(() -> new BadRequestRestApiException()
                                .developerMessage("Product item with id " + item.getProductItemPublicId() + " not found in product recon for route " + routePlanning.getRouteId())
                                .userMessage("Sorry. Product item not found for this route")))
                .toList();

        for (ProductItem productItem : productItems) {

            boolean notEnoughVanSaleQuantity = items.stream()
                    .anyMatch(item -> item.getProductItemPublicId().equals(productItem.getPublicId()) && item.getQuantitySold() > productItem.getVanSaleQuantity());

            if (notEnoughVanSaleQuantity) {
                throw new BadRequestRestApiException()
                        .developerMessage("Not enough van sale quantity for product item with id " + productItem.getPublicId())
                        .userMessage(String.format("Sorry. Not enough van sale quantity for product  %s. Available quantity: %d", productItem.getProductName(), productItem.getVanSaleQuantity()));
            }

            boolean matchAndNoDiscount = items.stream()
                    .anyMatch(item -> item.getProductItemPublicId().equals(productItem.getPublicId()) && item.getDiscountPerUnit() == null);

            if (requestSpecialDiscount && matchAndNoDiscount) {
                throw new BadRequestRestApiException()
                        .developerMessage("Discount per unit is required for product item with id " + productItem.getPublicId() + " when requesting special discount")
                        .userMessage("Sorry. Discount per unit is required for all items when requesting special discount");
            }
        }

        return productItems;

    }

    private RoutePlanning validateRoutePlanning(@NotNull UUID routePublicId) {
        return routePlanningRepository.findByPublicIdAndDeletedIsFalse(routePublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Route planning with id " + routePublicId + " not found")
                        .userMessage("Sorry. Route planning not found"));
    }

    private Customers validateCustomer(@NotNull UUID customerPublicId) {
        return customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer with id " + customerPublicId + " not found")
                        .userMessage("Sorry. Customer not found"));
    }

    private void isOrderAmendmentPresent(SalesOrders salesOrder) {
        CustomerReturn customerReturn = customerReturnRepository.findByDeletedIsFalseAndSalesOrder(salesOrder).orElse(null);

        if (customerReturn != null) {
            throw new BadRequestRestApiException()
                    .developerMessage(String.format("Customer return for the order %s already exists", salesOrder.getSalesOrderNumber()))
                    .userMessage(String.format("Customer return for the order %s already exists", salesOrder.getSalesOrderNumber()));
        }
    }

    // SalesOrders salesOrder, AmendOrderItemRequest amendOrderItemRequest, Principal principal
    public void createSalesOrderAmendment(CustomerReturn customerReturn) {
        SalesOrders salesOrder = customerReturn.getSalesOrder();
//        isOrderAmendmentPresent(salesOrder);

        DeliveryAgents deliveryAgent = getDeliveryAgent(salesOrder.getDeliveryAgent().getPublicId());

        RoutePlanning routePlanning = getRoute(salesOrder, deliveryAgent.getPublicId());

        ProductRecon productRecon = getProductRecon(routePlanning.getRouteId());

        OrderAmendment orderAmendment = new OrderAmendment();
        orderAmendment.setStatus(AmendmentStatus.Approved);
        orderAmendment.setRouteId(routePlanning.getRouteId());
        orderAmendment.setRoutePublicId(routePlanning.getPublicId().toString());
        orderAmendment.setAmendAction(AmendAction.FullyDeliveredWrongQuantity);
        orderAmendment.setDeliveryAgent(deliveryAgent);
        orderAmendment.setCreationTime(LocalDateTime.now());
        orderAmendment.setSalesOrder(salesOrder);
        orderAmendment.setDescription("customer returns");
        orderAmendment.setSalesOrderPublicId(salesOrder.getPublicId().toString());
        OrderAmendment orderAmendment1 = orderAmendmentRepository.save(orderAmendment);

        if (!customerReturn.getCustomerReturnItems().isEmpty()) {

            List<CustomerReturnItem> customerReturnItems = customerReturn.getCustomerReturnItems();

            Set<SalesOrderItems> orderItems = salesOrder.getSalesOrderItems();

            List<AmendmentProductItem> amendmentProductItems = new ArrayList<>();

            for (SalesOrderItems salesOrderItems : orderItems) {
                for (CustomerReturnItem itemRequest : customerReturnItems) {
                    if (salesOrderItems.getPublicId().equals(itemRequest.getOrderItemPublicId())) {

                        AmendmentProductItem amendmentProductItem = new AmendmentProductItem();
                        amendmentProductItem.setOrderAmendment(orderAmendment1);
//                        amendmentProductItem.setCreatedBy(updatedBy);
                        amendmentProductItem.setProductCode(salesOrderItems.getProduct().getSku());
                        amendmentProductItem.setProductName(salesOrderItems.getProduct().getProductName());
                        amendmentProductItem.setPricePerUnit(salesOrderItems.getPricePerUnit());

                        for (ProductItem productItem : productRecon.getProductItems()) {
                            if (productItem.getProductPublicId().equals(salesOrderItems.getProduct().getPublicId())) {
                                amendmentProductItem.setIssuedQuantity((int) productItem.getQuantityPicked());  // product item picked qty

                                amendmentProductItem.setDeliveredQuantity((int) productItem.getQuantitySold()); // product item delivered qty
                                amendmentProductItem.setAmendmentQuantity(itemRequest.getReturnedQuantity()); // returned qty

                            }
                        }  // 20   dev = 5, 15 -- return

                        //product quantity -

                        AmendmentProductItem amendmentProductItem1 = orderAmendmentItemRepository.save(amendmentProductItem);
                        amendmentProductItems.add(amendmentProductItem1);
                    }
                }
            }

        }
        OrderAmendment savedOrderAmendment = orderAmendmentRepository.save(orderAmendment1);

        salesOrdersRepository.save(salesOrder);

        String title = "Customer return Amendment Created";
        String message = String.format("Customer return amendment has been created by %s %s for order %s with action %s. Description: %s",
                deliveryAgent.getFirstName(),
                deliveryAgent.getLastName(),
                salesOrder.getSalesOrderNumber(),
                savedOrderAmendment.getAmendAction().name(),
                savedOrderAmendment.getDescription());

        notificationHelper.createNotificationHelper(
                title,
                message,
                "ROLE_ADMIN",
                savedOrderAmendment.getPublicId(),
                "ORDER_AMENDMENT",
                "APPROVE_DELIVERY_AGENT");
    }

    public Person validatePerson(String userName) {
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(userName);
        if (!personOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User not found")
                    .userMessage("Sorry. User not found");
        }
        return personOptional.get();
    }

    public CustomerReturn validateCustomerReturn(UUID customerReturnPublicId) {
        CustomerReturn customerReturn = customerReturnRepository.findByPublicIdAndDeletedIsFalse(customerReturnPublicId).orElse(null);
        if (customerReturn == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer return not found")
                    .userMessage("Sorry, customer return not found");
        }
        return customerReturn;
    }

    private void tumaUjumbeKwaDepartment(OrderAmendment savedOrderAmendment) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDateTime nairobiDate = LocalDateTime.now(nairobiZone);
        String actionTime = DateFormatter.formatDateToEastAfrica(nairobiDate);
        Person createdBy = savedOrderAmendment.getCreatedBy();

        String salesOrderNumber = savedOrderAmendment.getSalesOrder().getSalesOrderNumber();
        String customerName = savedOrderAmendment.getSalesOrder().getCustomer().getName();
        String DeliveryAgentName = savedOrderAmendment.getDeliveryAgent().getFullName();
        String routeId = savedOrderAmendment.getRouteId();

        String code = savedOrderAmendment.getRejectionsCode().getCode();
        String description = savedOrderAmendment.getRejectionsCode() != null ? String.format("-%s", savedOrderAmendment.getRejectionsCode().getDescription()) : "";
        String department = savedOrderAmendment.getRejectionsCode() != null ? (savedOrderAmendment.getRejectionsCode().getDepartment() != null ? savedOrderAmendment.getRejectionsCode().getDepartment().getName() : null) : null;

        String webHookTicketCreatedMessage = String.format("Amendment created for order %s, dated [%s] for customer %s. Reason code being “%s%s” actioned by (%s).",
                salesOrderNumber, actionTime, customerName, code, description, createdBy);
        String payload = String.format("{\"text\": \"%s\"}", webHookTicketCreatedMessage);

        if (department != null) {
            webhookClient.acceptAndSend(department, payload);
        }
    }

    public void sendAmendmentToDepartmentOfficers() throws IOException {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDateTime nairobiDate = LocalDateTime.now(nairobiZone);
        LocalDate today = LocalDate.now(nairobiZone);

        String actionTime = DateFormatter.formatDateToEastAfrica(nairobiDate);

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        log.info("-----check 1");

        List<Department> departmentList = departmentRepository.findByDeletedIsFalse();
        for (Department department : departmentList) {
            List<OrderAmendment> orderAmendmentList = orderAmendmentRepository.findByRejectionsCode_DepartmentAndCreationTimeBetween(department, startOfDay, endOfDay);

            if (!orderAmendmentList.isEmpty()) {
                File toFile = amendmentToExcel.convertToFile(orderAmendmentList, department.getName());
                log.info("-----check 2");
                amendmentToExcel.sendNotificationToDepartmentOfficer(toFile, department);

            }
        }
    }

    @Override
    public ByteArrayOutputStream runExportOrderAmendment(String searchParam, StatementExportType exportType, Pageable pageable, Principal principal) throws DocumentException, IOException {
        List<OrderAmendment> orderAmendments = getAllOrderAmendments(searchParam, pageable).getContent();
        if (exportType == StatementExportType.EXCEL) {
            return orderAmendmentExport.createExcel(orderAmendments, principal);
        } else {
            return orderAmendmentExport.generateLeadPdf(orderAmendments, principal);
        }
    }

    private void updateVanSales(RoutePlanning routePlanning, SalesOrders saved, ProductRecon productRecon, Person person) {

        Optional<OrderAmendment> orderAmendmentOptional = orderAmendmentRepository.findOrderAmendmentBySalesOrderPublicIdAndDeletedIsFalse(saved
                .getPublicId()
                .toString());

        if (!orderAmendmentOptional.isPresent()) {
            log.info("No order amendment found for sales order found");
            return;
        }

        OrderAmendment orderAmendment = orderAmendmentOptional.get();

        DeliveryAgents deliveryAgent = saved.getDeliveryAgent();

        SalesOrders salesOrders = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(saved.getPublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sales order with id " + saved.getPublicId() + " not found")
                        .userMessage("Sorry. Sales order not found"));

        List<SalesOrderItems> salesOrderItems = salesOrders.getSalesOrderItems().stream()
                .filter(this::shouldConvertOrderItemToVanSale)
                .toList();

        log.info("Sales order items found: {}", salesOrderItems.size());

        List<ProductItem> productItemList = new ArrayList<>();

        if (!salesOrderItems.isEmpty() && productRecon != null) {
            salesOrderItems.forEach(item -> addOrderItemToVanSales(routePlanning, salesOrders, deliveryAgent, productRecon, item)
                    .ifPresent(productItemList::add));

            productRecon.setUpdateDate(LocalDateTime.now());
            productReconRepository.save(productRecon);
        } else {
            log.info("No sales order items found for sales order {} and route {}",
                    salesOrders.getSalesOrderNumber(),
                    routePlanning.getRouteId());
        }

        buildVanSaleDeliveryAgentOperation(routePlanning,
                productItemList,
                salesOrders,
                person);
    }

    private boolean shouldConvertOrderItemToVanSale(SalesOrderItems item) {
        return !item.isVanSaleConverted()
                && (item.getStatus() == OrderItemStatus.NotDelivered
                || item.getStatus() == OrderItemStatus.PartiallyDelivered);
    }

    private Optional<ProductItem> addOrderItemToVanSales(RoutePlanning routePlanning,
                                                         SalesOrders salesOrders,
                                                         DeliveryAgents deliveryAgent,
                                                         ProductRecon productRecon,
                                                         SalesOrderItems item) {
        long quantityToConvert = item.getQuantity() - item.getQuantityDelivered();
        if (quantityToConvert <= 0) {
            item.setVanSaleConverted(true);
            salesOrderItemsRepository.save(item);
            return Optional.empty();
        }

        ProductItem productItem = productItemRepository
                .findByProductReconAndProductPublicIdAndDeletedIsFalse(productRecon, item.getProduct().getPublicId())
                .orElse(null);
        if (productItem == null) {
            log.info("Product item {} not found for van sale", item.getProduct().getPublicId());
            return Optional.empty();
        }

        if (wasOrderItemAlreadyConvertedToVanSale(routePlanning, salesOrders, item, productItem, quantityToConvert)) {
            item.setVanSaleConverted(true);
            salesOrderItemsRepository.save(item);
            log.info("Skipping duplicate van sale conversion for order {} product {}",
                    salesOrders.getSalesOrderNumber(),
                    item.getProduct().getSku());
            return Optional.empty();
        }

        log.info("Updating product item {} for van sale", productItem.getPublicId());
        productItem.setHasVanSale(Boolean.TRUE);
        productItem.setVanSaleItem(Boolean.TRUE);
        productItem.setProductSellingPrice(item.getPricePerUnit());
        productItem.setVanSaleQuantity(productItem.getVanSaleQuantity() + quantityToConvert);
        ProductItem productItem1 = productItemRepository.save(productItem);
        VanSales vanSales = getVansales(deliveryAgent, routePlanning);
        vanSales.addItem(productItem1);
        productItem1.setVanSales(vanSales);
        productItem1.setVanSaleItem(true);

        ProductItem saveProductItem = productItemRepository.save(productItem1);
        vanSalesRepository.save(vanSales);

        item.setVanSaleConverted(true);
        salesOrderItemsRepository.save(item);
        log.info("Product item {} updated for van sale", productItem1.getProductName());
        log.info("==============product has van sale {}================", productItem1.isVanSaleItem());
        return Optional.of(saveProductItem);
    }

    private boolean wasOrderItemAlreadyConvertedToVanSale(RoutePlanning routePlanning,
                                                          SalesOrders salesOrders,
                                                          SalesOrderItems item,
                                                          ProductItem productItem,
                                                          long quantityToConvert) {
        if (item.isVanSaleConverted()) {
            return true;
        }

        return deliveryAgentOperationRepository
                .findByRouteIdAndOrderNumberAndSkuAndDeletedIsFalse(
                        routePlanning.getRouteId(),
                        salesOrders.getSalesOrderNumber(),
                        item.getProduct().getSku())
                .map(operation -> salesOrders.getSalesOrderStatus() == LoadingStatusEnum.FAILED_DELIVERY
                        && operation.getPreBookedVanQuantity() >= quantityToConvert
                        && productItem.getVanSaleQuantity() >= quantityToConvert
                        && productItem.isVanSaleItem())
                .orElse(false);
    }

    private void buildVanSaleDeliveryAgentOperation(RoutePlanning routePlanning,
                                                    List<ProductItem> items,
                                                    SalesOrders salesOrders,
                                                    Person person) {

        for (ProductItem item : items) {

            DeliveryAgentOperation operation = deliveryAgentOperationRepository.findByRouteIdAndOrderNumberAndSkuAndDeletedIsFalse(routePlanning.getRouteId(), salesOrders.getSalesOrderNumber(), item.getProductCode()).orElse(null);

            if (operation != null) {
                operation.setPreBookedVanQuantity((int) item.getVanSaleQuantity());
                operation.setCreatedBy(person);
                deliveryAgentOperationRepository.save(operation);
            }
        }
    }

    private void buildVanSaleSoldOrderOperation(RoutePlanning routePlanning, DeliveryAgents deliveryAgent, UUID publicId, SalesOrders vanSaleOrder) {
        for (SalesOrderItems salesOrderItem : vanSaleOrder.getSalesOrderItems()) {
            DeliveryAgentOperation operation = deliveryAgentOperationRepository.findByRouteIdAndOrderNumberAndSkuAndDeletedIsFalse(routePlanning.getRouteId(), vanSaleOrder.getSalesOrderNumber(), salesOrderItem.getProduct().getSku()).orElse(new DeliveryAgentOperation());

            operation.setDeliveryAgentPublicId(deliveryAgent.getPublicId());
            operation.setRouteId(routePlanning.getRouteId());
            operation.setProductName(salesOrderItem.getProduct().getProductName());
            operation.setSku(salesOrderItem.getProduct().getSku());
            operation.setWarehousePublicId(vanSaleOrder.getWarehouse().getPublicId());
            operation.setOrderNumber(vanSaleOrder.getSalesOrderNumber());
            operation.setVanBookedQuantity(0);
            operation.setTotalQuantityPicked(salesOrderItem.getQuantityDelivered());
            operation.setPreBookedVanQuantity(0);
            operation.setVanSoldQuantity(salesOrderItem.getQuantityDelivered());
            operation.setCreatedBy(vanSaleOrder.getCreatedBy());
            operation.setVanSale(true);
            deliveryAgentOperationRepository.save(operation);
        }
    }


    // sell vansaleItems


    private SalesOrders sellVansalesItems(List<VanItemRequest> itemRequests, RoutePlanning routePlanning, SalesOrders order) {
        ProductRecon productRecon = productReconRepository.findByDeliveryAgentPublicIdIgnoreCaseAndDeliveryIdIgnoreCaseAndDeletedIsFalse(routePlanning.getDeliveryAgentId(), routePlanning.getRouteId()).
                orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Failed")
                        .userMessage("Failed"));

        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(productRecon.getWarehousePublicId())).orElseThrow(
                () -> new BadRequestRestApiException()
                        .developerMessage("Failed")
                        .userMessage("Failed"));

        DeliveryAgents agent = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(productRecon.getDeliveryAgentPublicId())).orElseThrow(() ->
                new BadRequestRestApiException()
                        .developerMessage("failed")
                        .userMessage("Failed"));

        Map<UUID, Integer> quantitySoldByProduct = new HashMap<>();
        itemRequests.forEach(item -> {
            if (item.getQuantitySold() > 0) {
                Optional<ProductItem> productItemOptional = productRecon.getProductItems().stream()
                        .filter(ProductItem::isVanSaleItem)
                        .filter(productItem -> productItem.getPublicId().equals(item.getProductItemPublicId()))
                        .findFirst();

                if (productItemOptional.isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Product item with id " + item.getProductItemPublicId() + " not found")
                            .userMessage("Sorry. Product item not found as van sale item");
                }

                ProductItem productItem = productItemOptional.get();
                if (item.getQuantitySold() > productItem.getVanSaleQuantity()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Quantity sold must be less than or equal to " + productItem.getVanSaleQuantity())
                            .userMessage("Sorry. Quantity sold must be less than or equal to " + productItem.getVanSaleQuantity());
                }
                productItem.setQuantitySold(productItem.getQuantitySold() + item.getQuantitySold());
                productItem.setVanSaleQuantity(productItem.getVanSaleQuantity() - item.getQuantitySold());
                productItem.setUpdateDate(LocalDateTime.now());
                productItem.setVanSaleItemSold(productItem.getVanSaleItemSold() + item.getQuantitySold());
                ProductItem productItem1 = productItemRepository.save(productItem);

                quantitySoldByProduct.merge(productItem1.getProductPublicId(), item.getQuantitySold(), Integer::sum);

                liveStockHelper.updateLiveStockValue(UUID.fromString(productRecon.getWarehousePublicId()),
                        productItem1.getProductPublicId(),
                        item.getQuantitySold(),
                        ProductUpdateType.SOLD_STOCK,
                        LocalDate.now());

                Product product = productRepository.findByPublicIdAndDeletedIsFalse(productItem1.getProductPublicId())
                        .orElseThrow(() -> new BadRequestRestApiException()
                                .developerMessage("Product with id " + productItem1.getProductPublicId() + " not found")
                                .userMessage("Sorry. Product not found"));

                stockMovementHelper.recordStockMovement(
                        product,
                        item.getQuantitySold(),
                        StockMovementTransactionType.DELIVERY,
                        TransactionBoundType.RECORD,
                        "Delivery agent " + productRecon.getDeliveryAgent(),
                        warehouse);
            }
        });

        warehouseService.createReconciliation(productRecon, routePlanning.getPublicId().toString(), true);
        SalesOrders saved = salesOrdersRepository.findByPublicId(order.getPublicId());
        saved.getSalesOrderItems().forEach(salesOrderItem -> {
            quantitySoldByProduct.forEach((productId, quantitySold) -> {
                if (productId.equals(salesOrderItem.getProductId())) {
                    salesOrderItem.setQuantityDelivered(quantitySold);
                    salesOrderItem.setStatus(OrderItemStatus.Delivered);
                    salesOrderItem.setUpdateDate(LocalDateTime.now());
                    salesOrderItemsRepository.save(salesOrderItem);
                }
            });
        });

        saved = salesOrdersRepository.save(saved);
        ensureVanSaleOrderItemsDelivered(saved);

        try {
            markOrderAsDelivered(order.getPublicId(), routePlanning.getPublicId(), null, agent.getPhoneNumber());
        } catch (IOException | InterruptedException e) {
            throw new BadRequestRestApiException()
                    .developerMessage("interupted")
                    .userMessage("Failed, {}", e.getMessage());
        }

        SalesOrders deliveredOrder = salesOrdersRepository.findByPublicId(order.getPublicId());
        ensureVanSaleOrderDelivered(deliveredOrder);
        updateVanSaleSoldQuantities(routePlanning, deliveredOrder);
        return deliveredOrder;
    }

    private void ensureVanSaleOrderItemsDelivered(SalesOrders order) {
        List<SalesOrderItems> undeliveredItems = order.getSalesOrderItems().stream()
                .filter(item -> item.getQuantityDelivered() != item.getQuantity()
                        || item.getStatus() != OrderItemStatus.Delivered)
                .toList();

        if (!undeliveredItems.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Van sale order " + order.getSalesOrderNumber() + " has undelivered items after sale")
                    .userMessage("Sorry. Van sale order was not delivered successfully");
        }
    }

    private void ensureVanSaleOrderDelivered(SalesOrders order) {
        if (order == null
                || order.getSalesOrderStatus() != LoadingStatusEnum.DELIVERED
                || order.getStatus() != OrderStatusEnum.Delivered) {
            throw new BadRequestRestApiException()
                    .developerMessage("Van sale order was not marked as delivered successfully")
                    .userMessage("Sorry. Van sale order was not delivered successfully");
        }

        if (order.getOrderType() == OrderType.CASH && order.getPaymentStatus() != PaymentStatusEnum.PAID) {
            throw new BadRequestRestApiException()
                    .developerMessage("Cash van sale order was delivered but payment status is not PAID")
                    .userMessage("Sorry. Van sale order payment was not completed successfully");
        }
    }
}
