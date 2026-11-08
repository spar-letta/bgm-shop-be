package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.dataTypes.Module;
import com.app.itibari.models.CustomerReturnItem;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.*;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.helpers.*;
import com.app.itibari.utils.reportHelper.CustomerReturnExport;
import com.app.itibari.utils.reportHelper.InvoiceReceipt;
import com.app.itibari.utils.reportHelper.TraInvoiceReceipt;
import com.app.itibari.utils.specification.CustomerReturnSpecification;
import com.app.itibari.utils.specification.ProductReconSpecification;
import com.app.itibari.utils.specification.SalesOrdersSpecification;
import com.itextpdf.io.source.ByteArrayOutputStream;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOrdersServiceImpl implements SalesOrdersService {

    private final SalesOrdersRepository salesOrdersRepository;

    private final PersonRepository personRepository;

    private final SalesOrdersServiceHelper salesOrdersServiceHelper;

    private final OrderCutOffTimeRepository orderCutOffTimeRepository;

    private final RoutePlanningRepository routePlanningRepository;

    private final CustomersRepository customersRepository;

    private final WarehouseRepository warehouseRepository;

    private final CounterService counterService;

    private final ProductRepository productRepository;

    private final SalesOrderItemsRepository salesOrderItemsRepository;

    private final SalesAgentsRepository salesAgentsRepository;

    private final WarehouseGetterHelper warehouseGetterHelper;

    private final DiscountHelper discountHelper;

    private final DiscountApprovalRepository discountApprovalRepository;

    private final EventsRepository eventsRepository;

    private final DiscountRepository discountRepository;

    private final RequestItemRepository requestItemRepository;

    private final StockUpdateHelper stockUpdateHelper;

    private final DayDateCheckerHelper dayDateCheckerHelper;

    private final SalesOrderAgentRepository salesOrderAgentRepository;

    private final CustomerCheckerHelper customerCheckerHelper;
    private final RejectionCodeRepository rejectionCodeRepository;
    private final StockMovementHelper stockMovementHelper;
    private final BnplHelper bnplHelper;
    private final InvoiceReceipt invoiceReceipt;
    private final TraInvoiceReceipt traInvoiceReceipt;
    private final SupplierRepository supplierRepository;
    private final TraUtilHelper traUtilHelper;
    private final TraSubmissionHelper traSubmissionHelper;
    private final TumaMessage tumaMessage;
    private final LiveStockHelper liveStockHelper;
    private final ProductStockHelper productStockHelper;
    private final ProductReconRepository productReconRepository;
    private final ProductItemRepository productItemRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final TraSwitchService traSwitchService;
    private final DeliveryAgentsRepository deliveryAgentsRepository;
    private final CustomerReturnRepository customerReturnRepository;
    private final CustomerReturnExport customerReturnExport;
    private final CreditRuleRepository creditRuleRepository;
    private final CreditLimitItemRepository creditLimitItemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
    private final LocalDateTime nairobiDate = LocalDateTime.now(nairobiZone);


    @Override
    @Transactional
    public SalesOrders createSalesOrder(@Valid SalesOrdersRequest salesOrdersRequest, String name) {

        BigDecimal creditLimitBalance = BigDecimal.ZERO;
        customerCheckerHelper.checkIfCustomerHasActiveOrder(salesOrdersRequest.getCustomerId());
        customerCheckerHelper.checkIfCustomerHasOverdueCreditMoreTHanSevenDays(salesOrdersRequest.getCustomerId());

        if (salesOrdersRequest.getBuyOnCredit() == Boolean.TRUE) {
            creditLimitBalance = customerCheckerHelper.checkIfCustomerIsAllowedToBuyOnCredit(salesOrdersRequest.getCustomerId(), salesOrdersRequest.isOrderBookedByParent());
        }

        SalesAgents salesAgent = null;
        Person person = null;
        boolean isSalesAgent = salesOrdersRequest.isBookedBySalesAgent();
        if (salesOrdersRequest.isBookedBySalesAgent()) {
            if (salesOrdersRequest.getSalesAgentPublicId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales agent public ID is required when booked by sales agent")
                        .userMessage("Sales agent public ID is required");
            }

            Optional<SalesAgents> salesAgentOptional = salesAgentsRepository.findByPublicIdAndDeletedIsFalse(salesOrdersRequest.getSalesAgentPublicId());
            if (salesAgentOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales agent with public ID " + salesOrdersRequest.getSalesAgentPublicId() + " not found")
                        .userMessage("Sales agent not found");
            }
            salesAgent = salesAgentOptional.get();
        } else {
            log.info("Sales agent not found {}", name);
            Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
            if (personOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Person with email " + name + " not found")
                        .userMessage("Person not found");
            }
            person = personOptional.get();
        }

        // Validate customer

        if (Validators.anyEqualNull(salesOrdersRequest.getSalesOrderItems(), salesOrdersRequest.getCustomerId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order items and customer ID must not be null")
                    .userMessage("Sales order items and customer ID are required");
        }

        Optional<Customers> customersOptional = customersRepository.findByPublicIdAndDeletedIsFalse(salesOrdersRequest.getCustomerId());
        if (customersOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with public ID " + salesOrdersRequest.getCustomerId() + " not found")
                    .userMessage("Customer not found");
        }

        Customers customer = customersOptional.get();

        if (customer.getStatus() == CustomerStatus.De_Activated) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer is De_Activated")
                    .userMessage("Sorry!. Customer is De_Activated");
        }

        boolean hasValidTin = customerCheckerHelper.validateCustomerTinNumber(customer);

        Set<SalesOrderItemsRequest> salesOrderItems = salesOrdersRequest.getSalesOrderItems();

        Map<UUID, Integer> productSkuMap = new HashMap<>();

        // add unique product SKUs and their quantities
        salesOrderItems.forEach(item -> {
            if (item.getProductId() == null || item.getQuantity() <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("ProductId and Quantity must be provided and Quantity must be greater than zero")
                        .userMessage("ProductId and Quantity must be provided and Quantity must be greater than zero");
            }
        });
        salesOrderItems.forEach(item -> {
            if (productSkuMap.containsKey(item.getProductId())) {
                productSkuMap.put(item.getProductId(), productSkuMap.get(item.getProductId()) + item.getQuantity());
            } else {
                productSkuMap.put(item.getProductId(), item.getQuantity());
            }
        });

        OrganizationRegions organizationRegion = customersRepository.findOrganizationRegionByCustomerPublicId(customer.getPublicId());

        if (organizationRegion == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with public ID " + customer.getPublicId() + " not found")
                    .userMessage("Customer's organization region not found");
        }
        Warehouse warehouse = warehouseGetterHelper.getWarehouse(organizationRegion);

        if (warehouse == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse not found for customer with public ID " + customer.getPublicId())
                    .userMessage("No warehouse found associated to any warehouse region");
        }

        // validate each product SKU and quantity
        productSkuMap.forEach((productId, quantity) -> {
            Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(productId);
            if (productOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product with publicId " + productId + " not found")
                        .userMessage("Product not found");
            }
            Product product = productOptional.get();
            if (product.getSellPrice() == null || product.getSellPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product with publicId " + productId + " has invalid sell price")
                        .userMessage(String.format("Product %s has invalid sell price", product.getProductName()));
            }
            stockUpdateHelper.checkIfProductIsAvailable(product, quantity, warehouse);
        });
        SalesOrders salesOrder = new SalesOrders();
        salesOrder.setOrganizationRegions(organizationRegion);
        salesOrder.setWarehouse(warehouse);

        if (isSalesAgent) {
            salesOrder.setSalesAgentPublicId(String.valueOf(salesOrdersRequest.getSalesAgentPublicId()));
            salesOrder.setBookedBy(salesAgent.getName());
            salesOrder.setSourceChannel(SourceChannelEnum.SALES_AGENTS_APP);
            salesOrder.setBookedBySalesAgent(true);
        } else {
            salesOrder.setSourceChannel(getSourceChannel(salesOrdersRequest.getSourceChannel()));
            salesOrder.setBookedBy(salesOrdersRequest.getBookedBy() != null ? salesOrdersRequest.getBookedBy() : person.getFullName());
            if (customer.getServedBy() != null) {
                salesOrder.setSalesAgentPublicId(String.valueOf(customer.getServedBy().getPublicId()));
            }
        }
        salesOrder.setCustomer(customer);
        salesOrder.setSalesOrderNumber("ORD" + counterService.getNextCounter(CounterType.SALES_ORDER));
        salesOrder.setExpectedDeliveryDate(LocalDate.now().plusDays(3));
        salesOrder.setPaymentStatus(PaymentStatusEnum.PENDING);
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.ORDER_PLACED);
        salesOrder.setSalesOrderItems(new HashSet<>());
        salesOrder.setCreatedBy(person);
        salesOrder.setBookedBySalesAgent(salesOrder.isBookedBySalesAgent());
        salesOrder.setCreationTime(LocalDateTime.now());
        salesOrder.setOrganizationRegions(organizationRegion);
        salesOrder.setCreditApplied(salesOrdersRequest.getBuyOnCredit());
        salesOrder.setHasValidTin(hasValidTin);

        if (salesOrdersRequest.isOrderBookedByParent()) {
            salesOrder.setBookedBy(customer.getParentCustomer() != null ? customer.getParentCustomer().getName() : salesOrdersRequest.getBookedBy());
            salesOrder.setBookedByParent(customer.getParentCustomer() != null && salesOrdersRequest.isOrderBookedByParent());
        }

        SalesOrders saved = salesOrdersRepository.save(salesOrder);

        productSkuMap.forEach((productId, quantity) -> {
            Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(productId);

            if (productOptional.isPresent()) {
                Product product = productOptional.get();

                BigDecimal taxAmount = Optional.ofNullable(product.getTaxAmount()).orElse(BigDecimal.ZERO);
                BigDecimal sellPrice = Optional.ofNullable(product.getSellPrice()).orElse(BigDecimal.ZERO);


                // this amount is subject to change if customer delivered quantity becomes less!
                BigDecimal taxCodeAmount = taxAmount.multiply(BigDecimal.valueOf(quantity));
                BigDecimal totalAmount = sellPrice.multiply(BigDecimal.valueOf(quantity));

                BigDecimal taxPercentage = product.getTaxCode() != null ? product.getTaxCode().getCodeValue() : BigDecimal.ZERO;

                SalesOrderItems salesOrderItem = SalesOrderItems.builder()
                        .productId(productId)
                        .quantity(quantity)
                        .pricePerUnit(sellPrice)
                        .status(OrderItemStatus.NotDelivered)
                        .totalAmount(totalAmount)
                        .product(product)
                        .taxAmountValue(taxCodeAmount)
                        .productBeforePriceAfterTax(product.getSellingPriceBeforeTax())
                        .taxPercentageApplied(taxPercentage)
                        .taxApplied(taxPercentage.compareTo(BigDecimal.ZERO) > 0)
                        .build();

                salesOrderItem.setSalesOrder(saved);
                salesOrderItem.setSalesOrderPublicId(saved.getPublicId());
                salesOrderItem.setSalesAgentPublicId(salesOrder.getSalesAgentPublicId());

                SalesOrderItems savedSalesOrderItem = salesOrderItemsRepository.save(salesOrderItem);
                saved.addSalesOrderItem(savedSalesOrderItem);
            }
        });

        saved.setTotalAmount(saved
                .getSalesOrderItems()
                .stream()
                .map(SalesOrderItems::getTotalPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        saved.calculateTotalTax();

        BigDecimal reduceAmount = saved.getSalesOrderItems().stream()
                .map(item -> Optional.ofNullable(item.getTaxAmountValue()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        saved.setTaxAmountValue(reduceAmount);

        boolean hasSpecialDiscount = checkIfCustomerHasSpecialDiscount(customer, salesOrdersRequest.getOtp());

        saved.setHasSpecialDiscount(hasSpecialDiscount);

        saved.setExpectedDeliveryDate(dayDateCheckerHelper.deliveryDateCalculator(LocalDate.now()));
        SalesOrders savedOrder = salesOrdersRepository.save(saved);

        Map<Product, Integer> reservedStockMap = new HashMap<>();
        savedOrder.getSalesOrderItems()
                .forEach(salesOrderItem -> {
                    Product product = salesOrderItem.getProduct();
                    Integer quantity = salesOrderItem.getQuantity();
                    reservedStockMap.put(product, quantity);
                });

        stockUpdateHelper.updateSalesOrderBookedStock(reservedStockMap, savedOrder.getWarehouse(),
                InventoryEvent.CREATE_ORDER.name());

        if (hasSpecialDiscount) {
            discountHelper.specialDiscount(savedOrder, customer);
        } else {
            discountHelper.addVolumeDiscount(savedOrder);
        }

        reservedStockMap.forEach((product, quantity) -> {
            stockMovementHelper.recordStockMovement(product,
                    quantity,
                    StockMovementTransactionType.ORDER_BOOKING,
                    TransactionBoundType.OUTGOING,
                    savedOrder.getBookedBy(),
                    savedOrder.getWarehouse());
        });

        SalesOrders finalSavedOrder = salesOrdersRepository.findByPublicId(salesOrder.getPublicId());

        BigDecimal amountPayable = finalSavedOrder.getTotalAmountPayable();

        if (finalSavedOrder.isCreditApplied() && creditLimitBalance.compareTo(amountPayable) >= 0) {
            bnplHelper.createUpdateCreditItems(finalSavedOrder, person);
            finalSavedOrder.setOrderType(OrderType.CREDIT);
        } else {
            finalSavedOrder.setOrderType(OrderType.CASH);
            finalSavedOrder.setCreditApplied(false);
        }

        String description = String.format("Sales order %s created with total amount %.2f, order type %s, credit applied %s, has valid Tin %s, has special discount %s",
                finalSavedOrder.getSalesOrderNumber(),
                finalSavedOrder.getTotalAmountPayable(),
                finalSavedOrder.getOrderType(),
                finalSavedOrder.isCreditApplied(),
                finalSavedOrder.isHasValidTin(),
                finalSavedOrder.isHasSpecialDiscount());

        eventsRepository.save(new Events(saved.getPublicId().toString(),
                "Order Created",
                LocalDateTime.now(),
                description,
                "CREATED",
                person));

        return salesOrdersRepository.save(finalSavedOrder);
    }

    private boolean checkIfCustomerHasSpecialDiscount(Customers customer, String otp) {

        List<Discounts> discountsList = discountRepository.findByCustomerPublicIdAndDiscountTypeAndStatusAndDeletedIsFalse(
                customer.getPublicId(),
                DiscountType.SpecialDiscount,
                DiscountStatus.Active);

        if (discountsList.isEmpty()) {
            return false;
        }
        long availableDiscounts = discountsList.stream()
                .filter(discounts -> discounts.getOrderPublicId() == null)
                .count();

        return availableDiscounts == 1 && otp != null;

    }

    private SourceChannelEnum getSourceChannel(String sourceChannel) {

        if (SourceChannelEnum.CUSTOMER_CARE_APP.name().equals(sourceChannel)) {
            return SourceChannelEnum.CUSTOMER_CARE_APP;
        } else if (SourceChannelEnum.SALES_AGENTS_APP.name().equals(sourceChannel)) {
            return SourceChannelEnum.SALES_AGENTS_APP;
        } else if (SourceChannelEnum.ONE_SALES.name().equals(sourceChannel)) {
            return SourceChannelEnum.ONE_SALES;
        } else {
            return SourceChannelEnum.BACK_OFFICE_APP;
        }
    }

    @Override
    public void updateSalesOrder(UUID salesOrderId, @Valid SalesOrdersRequest salesOrdersRequest, String name,
                                 Boolean isCustomer, Boolean isSalesAgentInitiated) {
        Person person;
        Customers customer;
        SalesAgents salesAgent;

        if (isCustomer == null) {
            isCustomer = Boolean.FALSE;
        }

        BigDecimal creditLimitBalance = BigDecimal.ZERO;

        if (salesOrdersRequest.getBuyOnCredit()) {
            creditLimitBalance = customerCheckerHelper.checkIfCustomerIsAllowedToBuyOnCredit(salesOrdersRequest.getCustomerId(), salesOrdersRequest.isOrderBookedByParent());
        }

        if (isCustomer) {
            Optional<Customers> optionalCustomer = customersRepository.findByPhoneNumberIgnoreCaseAndDeletedIsFalse(name);

            if (optionalCustomer.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Customer with phone number " + name + " not found")
                        .userMessage("Customer not found");
            }

            customer = optionalCustomer.get();
            person = null;
            salesAgent = null;
        } else if (salesOrdersRequest.isBookedBySalesAgent()) {
            Optional<SalesAgents> salesAgentOptional = salesAgentsRepository.findByPublicIdAndDeletedIsFalse(salesOrdersRequest.getSalesAgentPublicId());
            if (salesAgentOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales agent with public ID " + salesOrdersRequest.getSalesAgentPublicId() + " not found")
                        .userMessage("Sales agent not found");
            }
            salesAgent = salesAgentOptional.get();
            person = null;
            customer = null;
        } else {
            Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
            if (optionalPerson.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("User with email " + name + " not found")
                        .userMessage("User not found");
            }
            person = optionalPerson.get();
            customer = null;
            salesAgent = null;
        }

        Optional<SalesOrders> salesOrdersOptional = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrderId);
        if (salesOrdersOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales Order with public ID " + salesOrderId + " not found")
                    .userMessage("Sales Order not found");
        }

        SalesOrders forUpdate = salesOrdersOptional.get();

        if (forUpdate.getCustomer().getStatus() == CustomerStatus.De_Activated) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer is De_Activated")
                    .userMessage("Sorry!. Customer is De_Activated");
        }

        creditLimitBalance = creditLimitBalance.add(forUpdate.getTotalAmountPayable());


        if (forUpdate.getSalesOrderStatus() != LoadingStatusEnum.ORDER_PLACED) {
            throw new BadRequestRestApiException()
                    .developerMessage("Only orders with status ORDER_PLACED can be updated")
                    .userMessage("Only orders with status ORDER_PLACED can be updated");
        }


        salesOrdersServiceHelper.validateSalesOrderRequest(salesOrdersRequest, forUpdate);

        Map<Product, Integer> reservedStockMap = forUpdate.getSalesOrderItems()
                .stream()
                .collect(Collectors.toMap(
                        SalesOrderItems::getProduct,
                        SalesOrderItems::getQuantity
                ));

        String doneBy;
        if (isCustomer) {
            doneBy = customer.getName();
        } else if (salesOrdersRequest.isBookedBySalesAgent() && salesAgent != null) {
            doneBy = salesAgent.getName();
        } else {
            doneBy = person != null ? person.getFullName() : "System";
        }

        stockUpdateHelper.revertToPreviousState(reservedStockMap, forUpdate.getWarehouse(), doneBy);

        salesOrderItemsRepository.deleteAll(forUpdate.getSalesOrderItems());

        Set<SalesOrderItems> salesOrderItems = new HashSet<>();
        salesOrdersRequest.getSalesOrderItems().forEach(salesOrderItemsRequest -> {
            Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(salesOrderItemsRequest.getProductId());
            if (productOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product with public ID " + salesOrderItemsRequest.getProductId() + " not found")
                        .userMessage("Product not found");
            }
            Product product = productOptional.get();
            BigDecimal taxPercentage = product.getTaxCode() != null ? product.getTaxCode().getCodeValue() : BigDecimal.ZERO;
            BigDecimal taxCodeAmount = product.getTaxAmount().multiply(BigDecimal.valueOf(salesOrderItemsRequest.getQuantity()));
            SalesOrderItems salesOrderItem = SalesOrderItems.builder()
                    .productId(salesOrderItemsRequest.getProductId())
                    .quantity(salesOrderItemsRequest.getQuantity())
                    .pricePerUnit(product.getSellPrice())
                    .status(OrderItemStatus.NotDelivered)
                    .totalAmount(product.getSellPrice().multiply(new BigDecimal(salesOrderItemsRequest.getQuantity())))
                    .product(product)
                    .salesOrder(forUpdate)
                    .salesOrderPublicId(forUpdate.getPublicId())
                    .salesAgentPublicId(forUpdate.getSalesAgentPublicId())
                    .taxAmountValue(taxCodeAmount)
                    .productBeforePriceAfterTax(product.getSellingPriceBeforeTax())
                    .taxPercentageApplied(taxPercentage)
                    .taxApplied(taxPercentage.compareTo(BigDecimal.ZERO) > 0)
                    .build();

            SalesOrderItems savedSalesOrderItem = salesOrderItemsRepository.save(salesOrderItem);

            salesOrderItems.add(savedSalesOrderItem);

        });

        forUpdate.setSalesOrderItems(salesOrderItems);
        forUpdate.setTotalAmount(forUpdate
                .getSalesOrderItems()
                .stream()
                .map(SalesOrderItems::getTotalPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        forUpdate.calculateTotalTax();
        forUpdate.setUpdatedBy(person);
        forUpdate.setUpdateDate(LocalDateTime.now());
        forUpdate.setInvoice(null);
        forUpdate.setExpectedDeliveryDate(dayDateCheckerHelper.deliveryDateCalculator(LocalDate.now()));
        if (salesOrdersRequest.isOrderBookedByParent()) {
            forUpdate.setBookedBy(forUpdate.getCustomer().getParentCustomer() != null ? forUpdate.getCustomer().getParentCustomer().getName() : salesOrdersRequest.getBookedBy());
            forUpdate.setBookedByParent(forUpdate.getCustomer().getParentCustomer() != null && salesOrdersRequest.isOrderBookedByParent());
        }

        boolean hasSpecialDiscount = forUpdate.isHasSpecialDiscount() || checkIfCustomerHasSpecialDiscount(forUpdate.getCustomer(), salesOrdersRequest.getOtp());

        forUpdate.setHasSpecialDiscount(hasSpecialDiscount);
        if (hasSpecialDiscount) {
            discountHelper.specialDiscount(forUpdate, forUpdate.getCustomer());
        } else {
            discountHelper.addVolumeDiscount(forUpdate);
        }

        SalesOrders saved = salesOrdersRepository.findByPublicId(forUpdate.getPublicId());
        reservedStockMap.clear();

        // update booked stock

        forUpdate.getSalesOrderItems()
                .forEach(salesOrderItem -> {
                    Product product = salesOrderItem.getProduct();
                    Integer quantity = salesOrderItem.getQuantity();
                    reservedStockMap.put(product, quantity);
                });

        stockUpdateHelper.updateSalesOrderBookedStock(reservedStockMap, saved.getWarehouse(),
                InventoryEvent.UPDATE_ORDER.name());


        reservedStockMap.forEach((product, quantity) -> {
            stockMovementHelper.recordStockMovement(product,
                    quantity,
                    StockMovementTransactionType.ORDER_EDIT,
                    TransactionBoundType.OUTGOING,
                    doneBy,
                    saved.getWarehouse());
        });


        BigDecimal amountPayable = saved.getTotalAmountPayable();
        BigDecimal customerToPay = BigDecimal.ZERO;

        if (saved.isCreditApplied() && creditLimitBalance.compareTo(amountPayable) >= 0) {

            bnplHelper.createUpdateCreditItems(saved, person);
            saved.setOrderType(OrderType.CREDIT);
            saved.setCustomerAmountToPay(customerToPay);
        } else {
            saved.setOrderType(OrderType.CASH);
            saved.setCreditApplied(false);

        }

        String description = String.format("Sales order %s updated with total amount %.2f, order type %s, credit applied %s, has valid Tin %s, has special discount %s",
                saved.getSalesOrderNumber(),
                saved.getTotalAmountPayable(),
                saved.getOrderType(),
                saved.isCreditApplied(),
                saved.isHasValidTin(),
                saved.isHasSpecialDiscount());

        eventsRepository.save(new Events(salesOrderId.toString(),
                "Order Updated",
                LocalDateTime.now(),
                description,
                "UPDATED",
                isCustomer ? null : person));


        salesOrdersRepository.save(saved);
    }

    @Override
    public SalesOrders getSalesOrder(UUID salesOrderId) {

        Optional<SalesOrders> salesOrderOptional = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrderId);
        if (salesOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order with ID " + salesOrderId + " not found")
                    .userMessage("Sales order not found");
        }
        return salesOrderOptional.get();
    }

    @Override
    public SalesOrdersPageableDTO getAllSalesOrders(String searchParam,
                                                    String salesOrderNumber,
                                                    UUID customerId,
                                                    UUID warehouseId,
                                                    UUID organizationRegionId,
                                                    SourceChannelEnum sourceChannel,
                                                    LoadingStatusEnum salesOrderStatus,
                                                    PaymentStatusEnum paymentStatus,
                                                    String invoiceNumber,
                                                    LocalDate startDate,
                                                    LocalDate endDate,
                                                    UUID location,
                                                    Boolean bookedBySalesAgent,
                                                    UUID salesAgentPublicId,
                                                    UUID deliveryAgentPublicId,
                                                    UUID parentCustomerPublicId,
                                                    Boolean isCredit,
                                                    List<LoadingStatusEnum> statuses,
                                                    Boolean walkIn,
                                                    Boolean bookedByParent,
                                                    Pageable pageable) {

        Specification<SalesOrders> spec = Specification
                .where(SalesOrdersSpecification.searchParamMatches(searchParam))
                .and(SalesOrdersSpecification.isNotDeleted())
                .and(SalesOrdersSpecification.hasSalesOrderNumber(salesOrderNumber))
                .and(SalesOrdersSpecification.hasCustomer(customerId))
                .and(SalesOrdersSpecification.hasWarehouse(warehouseId))
                .and(SalesOrdersSpecification.hasOrganizationRegion(organizationRegionId))
                .and(SalesOrdersSpecification.hasSourceChannel(sourceChannel))
                .and(SalesOrdersSpecification.hasSalesOrderStatus(salesOrderStatus))
                .and(SalesOrdersSpecification.hasPaymentStatus(paymentStatus))
                .and(SalesOrdersSpecification.hasInvoiceNumber(invoiceNumber))
                .and(SalesOrdersSpecification.hasLocation(location))
                .and(SalesOrdersSpecification.hasSalesAgent(salesAgentPublicId))
                .and(SalesOrdersSpecification.hasDeliveryAgent(deliveryAgentPublicId))
                .and(SalesOrdersSpecification.bookedBySalesAgent(bookedBySalesAgent))
                .and(SalesOrdersSpecification.hasParentCustomer(parentCustomerPublicId))
                .and(SalesOrdersSpecification.isCredit(isCredit))
                .and(SalesOrdersSpecification.hasStatuses(statuses))
                .and(SalesOrdersSpecification.isWalkIn(walkIn))
                .and(SalesOrdersSpecification.isBookedByParent(bookedByParent))
                .and(SalesOrdersSpecification.hasFromToDates(startDate, endDate));

        Page<SalesOrders> allOrders = salesOrdersRepository.findAll(spec, pageable);

        BigDecimal totalOrderValue = BigDecimal.ZERO;
        if (!allOrders.isEmpty()) {
            totalOrderValue = allOrders.stream()
                    .map(SalesOrders::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        }

        int size = allOrders.getSize();
        SalesOrdersPageableDTO salesOrdersPageableDTO = new SalesOrdersPageableDTO();
        salesOrdersPageableDTO.setTotalElements(allOrders.getTotalElements());
        salesOrdersPageableDTO.setPageSize(allOrders.getSize());
        salesOrdersPageableDTO.setTotalPages(allOrders.getTotalPages());
        salesOrdersPageableDTO.setLast(allOrders.isLast());
        salesOrdersPageableDTO.setFirst(allOrders.isFirst());
        salesOrdersPageableDTO.setPageNumber(allOrders.getNumber());
        salesOrdersPageableDTO.setTotalOrderValue(totalOrderValue);
        salesOrdersPageableDTO.setContent(allOrders.getContent());
        return salesOrdersPageableDTO;
    }

    @Override
    public void deleteSalesOrder(UUID salesOrderId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<SalesOrders> salesOrderOptional = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrderId);
        if (salesOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order with public Id " + salesOrderId + " not found")
                    .userMessage("Sales order not found");
        }

        SalesOrders salesOrder = salesOrderOptional.get();
        salesOrder.setDeleted(true);
        personOptional.ifPresent(salesOrder::setUpdatedBy);
        salesOrder.setUpdateDate(LocalDateTime.now());

        salesOrdersRepository.save(salesOrder);

        String description = "Sales order " + salesOrder.getSalesOrderNumber() + " deleted";
        eventsRepository.save(new Events(salesOrderId.toString(),
                "Order Deleted",
                LocalDateTime.now(),
                description,
                "DELETED",
                personOptional.orElse(null)));

        if (salesOrder.isCreditApplied()) {
            bnplHelper.cancelCreditItemsForOrder(salesOrder, name);
        }
    }

    @Override
    @Transactional  // Important: ensures the bulk update runs in a transaction
    public void moveOrdersToPlanning() {
        orderCutOffTimeRepository.findOrderCutOffTimeByDeletedIsFalse().ifPresent(cutOffTime -> {
            LocalTime startTime = cutOffTime.getCutOffTime();
            LocalTime endTime = Optional.ofNullable(cutOffTime.getCutOffTimeEnd())
                    .orElse(startTime.plusHours(8));
            LocalTime now = LocalTime.now();

            log.info("Order cut off time window: {} - {}", startTime, endTime);

            boolean crossesMidnight = endTime.isBefore(startTime);
            boolean withinWindow;

            if (crossesMidnight) {
                withinWindow = !now.isBefore(startTime) || !now.isAfter(endTime);
            } else {
                withinWindow = !now.isBefore(startTime) && !now.isAfter(endTime);
            }

            if (withinWindow) {
                int updatedCount = salesOrdersRepository.bulkUpdateStatus(
                        LoadingStatusEnum.ORDER_PLACED,
                        LoadingStatusEnum.PLANNING
                );

                if (updatedCount > 0) {
                    log.info("Successfully moved {} orders from ORDER_PLACED to PLANNING", updatedCount);

                    // Optional: still log an event (single event for batch)
                    String description = "Bulk move: " + updatedCount + " orders moved to PLANNING status";
                    eventsRepository.save(new Events(
                            LocalDateTime.now().toString(),
                            "Bulk Order Moved to Planning",
                            LocalDateTime.now(),
                            description,
                            "PLANNING",
                            null
                    ));
                }
            } else {
                log.info(" updated {} orders. Current time {} is outside the cut-off window.", 0, now);
            }
        });
    }

    @Override
    public Page<SalesOrderItems> getSalesOrderItems(UUID salesOrderPublicId, Pageable pageable) {

        Optional<SalesOrders> optionalSalesOrders = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrderPublicId);

        if (optionalSalesOrders.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order with public ID " + salesOrderPublicId + " not found")
                    .userMessage("Sales order not found");
        }

        SalesOrders salesOrders = optionalSalesOrders.get();

        List<SalesOrderItems> salesOrderItems = salesOrders.getSalesOrderItems().stream().toList();
        return new PageImpl<>(salesOrderItems, pageable, salesOrders.getSalesOrderItems().size());
    }

    @Override
    public Page<SalesOrderItemSlimDTO> getSalesOrderItemsSlim(UUID salesOrderPublicId, Pageable pageable) {

        Optional<SalesOrders> optionalSalesOrders = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrderPublicId);

        if (optionalSalesOrders.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order with public ID " + salesOrderPublicId + " not found")
                    .userMessage("Sales order not found");
        }

        List<SalesOrderItemSlimDTO> slimItems = optionalSalesOrders.get()
                .getSalesOrderItems()
                .stream()
                .map(item -> {
                    SalesOrderItemSlimDTO dto = new SalesOrderItemSlimDTO();
                    dto.setPublicId(item.getPublicId());
                    dto.setQuantity(item.getQuantity());
                    dto.setQuantityDelivered(item.getQuantityDelivered());
                    dto.setPricePerUnit(item.getPricePerUnit());
                    if (item.getProduct() != null) {
                        ProductSlimDTO productSlim = new ProductSlimDTO();
                        productSlim.setPublicId(item.getProduct().getPublicId());
                        productSlim.setProductName(item.getProduct().getProductName());
                        productSlim.setSellPrice(item.getProduct().getSellPrice());
                        dto.setProduct(productSlim);
                    }

                    return dto;
                })
                .toList();

        return new PageImpl<>(slimItems, pageable, slimItems.size());
    }

    @Override
    public Page<SalesOrderItems> getSalesOrderItemsPerAgent(UUID deliveryAgentPublicId, Pageable pageable) {

        List<RouteStatus> statuses = List.of(RouteStatus.CREATED, RouteStatus.Loading);
        List<RoutePlanning> routePlannings = routePlanningRepository.findAllByDeliveryAgentIdIgnoreCaseAndRouteStatusesAndDeletedIsFalse(
                deliveryAgentPublicId.toString(),
                statuses);

        if (routePlannings.isEmpty()) {
            return null;
        }

        if (routePlannings.size() > 1) {
            throw new BadRequestRestApiException()
                    .developerMessage("Only one route planning per agent")
                    .userMessage("Sorry. Agent has more than one route");
        }

        RoutePlanning routePlanning = routePlannings.get(0);

        List<SalesOrders> salesOrders = routePlanning.getSalesOrders();

        List<SalesOrderItems> salesOrderItems = new ArrayList<>();

        salesOrders.forEach(salesOrder -> {
            salesOrderItems.addAll(salesOrder.getSalesOrderItems());
        });

        return new PageImpl<>(salesOrderItems, pageable, salesOrderItems.size());
    }

    /*
    mobile customer orders
     */

    @Transactional
    @Override
    public SalesOrders placeOrder(SalesOrdersRequest salesOrdersRequest) {

        Optional<Customers> optionalCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(salesOrdersRequest.getCustomerId());

        if (optionalCustomer.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with public ID " + salesOrdersRequest.getCustomerId() + " not found")
                    .userMessage("Customer not found");
        }
        Customers customer = optionalCustomer.get();

        if (customer.getStatus() == CustomerStatus.De_Activated) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer is De_Activated")
                    .userMessage("Sorry!. Customer is De_Activated");
        }

        customerCheckerHelper.checkIfCustomerHasActiveOrder(salesOrdersRequest.getCustomerId());

        customerCheckerHelper.checkIfCustomerHasOverdueCreditMoreTHanSevenDays(salesOrdersRequest.getCustomerId());

        boolean hasValidTin = customerCheckerHelper.validateCustomerTinNumber(customer);


        BigDecimal creditLimitBalance = BigDecimal.ZERO;

        if (salesOrdersRequest.getBuyOnCredit() == Boolean.TRUE) {
            creditLimitBalance = customerCheckerHelper.checkIfCustomerIsAllowedToBuyOnCredit(salesOrdersRequest.getCustomerId(), salesOrdersRequest.isOrderBookedByParent());
        }

        SalesOrders salesOrder = new SalesOrders();
        OrganizationRegions organizationRegion = customersRepository.findOrganizationRegionByCustomerPublicId(salesOrdersRequest.getCustomerId());
        if (null != organizationRegion) {
            salesOrder.setOrganizationRegions(organizationRegion);

            Warehouse warehouse = warehouseGetterHelper.getWarehouse(organizationRegion);
            salesOrder.setWarehouse(warehouse);
        }

        Set<SalesOrderItemsRequest> salesOrderItems = salesOrdersRequest.getSalesOrderItems();

        Map<UUID, Integer> productSkuMap = new HashMap<>();

        // add unique product SKUs and their quantities

        salesOrderItems.forEach(item -> {
            if (item.getProductId() == null || item.getQuantity() <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("ProductId and Quantity must be provided and Quantity must be greater than zero")
                        .userMessage("ProductId and Quantity must be provided and Quantity must be greater than zero");
            }
        });

        salesOrderItems.forEach(item -> {
            if (productSkuMap.containsKey(item.getProductId())) {
                productSkuMap.put(item.getProductId(), productSkuMap.get(item.getProductId()) + item.getQuantity());
            } else {
                productSkuMap.put(item.getProductId(), item.getQuantity());
            }
        });

        // validate each product SKU and quantity

        productSkuMap.forEach((productId, quantity) -> {
            Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(productId);
            if (productOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product with publicId " + productId + " not found")
                        .userMessage("Product not found");
            }
            Product product = productOptional.get();
            if (product.getSellPrice() == null || product.getSellPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product with publicId " + productId + " has invalid sell price")
                        .userMessage(String.format("Product %s has invalid sell price", product.getProductName()));
            }

            stockUpdateHelper.checkIfProductIsAvailable(product, quantity, salesOrder.getWarehouse());
        });

        boolean hasOtpAndIsValid = false;

        if (salesOrdersRequest.isBookedBySalesAgent()) {
            if (salesOrdersRequest.getSalesAgentPublicId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales agent public ID is required when booked by sales agent")
                        .userMessage("Missing sales agent details");
            }

            Optional<SalesAgents> optionalSalesAgent = salesAgentsRepository.findByPublicIdAndDeletedIsFalse(salesOrdersRequest.getSalesAgentPublicId());

            if (optionalSalesAgent.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales agent with public ID " + salesOrdersRequest.getSalesAgentPublicId() + " not found")
                        .userMessage("Sales agent not found");
            }
            SalesAgents salesAgent = optionalSalesAgent.get();

            if (salesOrdersRequest.getOtp() != null) {
                hasOtpAndIsValid = validateAgentOtp(salesAgent.getPublicId(),
                        salesOrdersRequest.getOtp(),
                        salesOrdersRequest.getCustomerId());
            }

            salesOrder.setBookedBy(salesAgent.getName());
            salesOrder.setSalesAgentPublicId(salesAgent.getPublicId().toString());
            salesOrder.setSourceChannel(SourceChannelEnum.SALES_AGENTS_APP);
            salesOrder.setBookedBySalesAgent(true);
        } else {
            salesOrder.setBookedBy(customer.getName());
            salesOrder.setSourceChannel(SourceChannelEnum.CUSTOMER_CARE_APP);
            if (customer.getServedBy() != null) {
                salesOrder.setSalesAgentPublicId(String.valueOf(customer.getServedBy().getPublicId()));
            }
        }
        salesOrder.setCustomer(customer);
        salesOrder.setSalesOrderNumber("ORD" + counterService.getNextCounter(CounterType.SALES_ORDER));
        salesOrder.setExpectedDeliveryDate(LocalDate.now().plusDays(3));
        salesOrder.setPaymentStatus(PaymentStatusEnum.PENDING);
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.ORDER_PLACED);
        salesOrder.setSalesOrderItems(new HashSet<>());
        salesOrder.setCreatedBy(customer.getCreatedBy());
        salesOrder.setOrganizationRegions(organizationRegion);
        salesOrder.setCreditApplied(salesOrdersRequest.getBuyOnCredit());
        salesOrder.setHasValidTin(hasValidTin);

        if (salesOrdersRequest.isOrderBookedByParent()) {
            salesOrder.setBookedBy(customer.getParentCustomer() != null ? customer.getParentCustomer().getName() : salesOrdersRequest.getBookedBy());
            salesOrder.setBookedByParent(customer.getParentCustomer() != null && salesOrdersRequest.isOrderBookedByParent());
        }

        SalesOrders saved = salesOrdersRepository.save(salesOrder);

        productSkuMap.forEach((productId, quantity) -> {
            Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(productId);
            if (productOptional.isPresent()) {
                Product product = productOptional.get();

                BigDecimal taxAmount = Optional.ofNullable(product.getTaxAmount()).orElse(BigDecimal.ZERO);
                BigDecimal sellPrice = Optional.ofNullable(product.getSellPrice()).orElse(BigDecimal.ZERO);

                BigDecimal taxCodeAmount = taxAmount.multiply(BigDecimal.valueOf(quantity));
                BigDecimal totalAmount = sellPrice.multiply(BigDecimal.valueOf(quantity));

                BigDecimal taxPercentage = product.getTaxCode() != null ? product.getTaxCode().getCodeValue() : BigDecimal.ZERO;
                SalesOrderItems salesOrderItem = SalesOrderItems.builder()
                        .productId(productId)
                        .quantity(quantity)
                        .pricePerUnit(product.getSellPrice())
                        .status(OrderItemStatus.NotDelivered)
                        .totalAmount(product.getSellPrice().multiply(new BigDecimal(quantity)))
                        .totalAmount(totalAmount)
                        .product(product)
                        .taxAmountValue(taxCodeAmount)
                        .productBeforePriceAfterTax(product.getSellingPriceBeforeTax())
                        .taxPercentageApplied(taxPercentage)
                        .taxApplied(taxPercentage.compareTo(BigDecimal.ZERO) > 0)
                        .build();
                salesOrderItem.setSalesOrder(saved);
                salesOrderItem.setSalesOrderPublicId(saved.getPublicId());
                salesOrderItem.setSalesAgentPublicId(salesOrder.getSalesAgentPublicId());
                SalesOrderItems savedSalesOrderItem = salesOrderItemsRepository.save(salesOrderItem);
                saved.addSalesOrderItem(savedSalesOrderItem);
            }
        });
        saved.setTotalAmount(saved
                .getSalesOrderItems()
                .stream()
                .map(SalesOrderItems::getTotalPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        saved.calculateTotalTax();

        saved.setExpectedDeliveryDate(dayDateCheckerHelper.deliveryDateCalculator(LocalDate.now()));

        BigDecimal reduceAmount = saved.getSalesOrderItems().stream()
                .map(item -> Optional.ofNullable(item.getTaxAmountValue()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        saved.setTaxAmountValue(reduceAmount);

        SalesOrders savedOrder = salesOrdersRepository.save(saved);

        boolean hasSpecialDiscount = checkIfCustomerHasSpecialDiscount(customer, salesOrdersRequest.getOtp());
        savedOrder.setHasSpecialDiscount(hasSpecialDiscount);
        salesOrdersRepository.save(savedOrder);

        Map<Product, Integer> reservedStockMap = new HashMap<>();

        savedOrder.getSalesOrderItems()
                .forEach(salesOrderItem -> {
                    Product product = salesOrderItem.getProduct();
                    Integer quantity = salesOrderItem.getQuantity();
                    reservedStockMap.put(product, quantity);
                });

        stockUpdateHelper.updateSalesOrderBookedStock(reservedStockMap, savedOrder.getWarehouse(),
                InventoryEvent.PLACE_ORDER_MOBILE.name());

        applyDiscounts(savedOrder, customer, hasOtpAndIsValid, salesOrdersRequest.getOtp() != null);

        String description = "Sales order " + savedOrder.getSalesOrderNumber() + " created";
        eventsRepository.save(new Events(saved.getPublicId().toString(),
                "Order Created",
                LocalDateTime.now(),
                description,
                "CREATED",
                null));


        reservedStockMap.forEach((product, quantity) -> {
            stockMovementHelper.recordStockMovement(product,
                    quantity,
                    StockMovementTransactionType.ORDER_BOOKING,
                    TransactionBoundType.OUTGOING,
                    savedOrder.getBookedBy(),
                    savedOrder.getWarehouse());
        });

        SalesOrders finalSavedOrder = salesOrdersRepository.findByPublicId(salesOrder.getPublicId());

        BigDecimal amountPayable = finalSavedOrder.getTotalAmountPayable();

        if (finalSavedOrder.isCreditApplied() && creditLimitBalance.compareTo(amountPayable) >= 0) {

            bnplHelper.createUpdateCreditItems(finalSavedOrder, null);
            finalSavedOrder.setOrderType(OrderType.CREDIT);
        } else {
            if (Boolean.TRUE.equals(salesOrdersRequest.getBuyOnCredit()) && finalSavedOrder.getCustomer().isHasParentCustomer()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Credit order exceeds the parent customer's available credit limit")
                        .userMessage("The requested credit amount exceeds the parent customer's available credit limit");
            }
            finalSavedOrder.setOrderType(OrderType.CASH);
            finalSavedOrder.setCreditApplied(false);
        }

        return salesOrdersRepository.save(finalSavedOrder);
    }

    private void applyDiscounts(SalesOrders order, Customers customer, boolean hasOtpAndIsValid, boolean hasOtp) {

        log.info("[DISCOUNT-TRACE] applyDiscounts entry: order={} customer={} hasOtp={} otpValid={} path={} totalAmount={}",
                order.getSalesOrderNumber(), customer.getPublicId(), hasOtp, hasOtpAndIsValid,
                (hasOtp && hasOtpAndIsValid) ? "SPECIAL" : "VOLUME", order.getTotalAmount());

        if (hasOtp && hasOtpAndIsValid) {
            try {
                discountHelper.specialDiscount(order, customer);
            } catch (Exception e) {
                log.error("[DISCOUNT-TRACE] Error while applying special discount for customer with public ID {} and order number {}",
                        customer.getPublicId(),
                        order.getSalesOrderNumber(),
                        e);
            }
        } else {
            try {
                discountHelper.addVolumeDiscount(order);

            } catch (Exception e) {
                log.error("[DISCOUNT-TRACE] Error while applying volume discount for customer with public ID {} and order number {}",
                        customer.getPublicId(),
                        order.getSalesOrderNumber(),
                        e);
            }
        }

        log.info("[DISCOUNT-TRACE] applyDiscounts exit: order={} totalDiscount={} totalAmountPayable={}",
                order.getSalesOrderNumber(), order.getTotalDiscount(), order.getTotalAmountPayable());
    }

    private boolean validateAgentOtp(UUID publicId, String otp, UUID customerId) {

        Optional<DiscountApproval> optionalDiscountApproval = discountApprovalRepository.findDiscountApprovalByCustomerPublicIdAndRequestedByPublicIdAndOtpAndDeletedIsFalse(
                customerId.toString(),
                publicId.toString(),
                otp);
        if (optionalDiscountApproval.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid OTP")
                    .userMessage("Invalid OTP");
        }
        DiscountApproval discountApproval = optionalDiscountApproval.get();
        if (discountApproval.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestRestApiException()
                    .developerMessage("OTP has expired")
                    .userMessage("OTP has expired");
        }

        if (discountApproval.isOtpUsed()) {
            throw new BadRequestRestApiException()
                    .developerMessage("OTP has already been used")
                    .userMessage("OTP has already been used");
        }

        discountApproval.setOtpUsed(true);
        discountApprovalRepository.save(discountApproval);

        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerId).orElse(null);
        if (customer != null) {
            customer.setSpecialDiscountApproved(false);
            customer.setSpecialDiscountRequested(false);
            customersRepository.save(customer);
        }

        return true;
    }

    @Override
    public Page<SalesOrders> getCustomerOrders(String searchParam,
                                               String salesOrderNumber,
                                               UUID warehouseId,
                                               UUID organizationRegionId,
                                               LoadingStatusEnum salesOrderStatus,
                                               PaymentStatusEnum paymentStatus,
                                               String invoiceNumber,
                                               Boolean isCredit,
                                               String startDate,
                                               String endDate,
                                               String name,
                                               Pageable pageable) {

        Optional<Customers> optionalCustomer = customersRepository.findByPhoneNumberAndDeletedIsFalse(name);

        if (optionalCustomer.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with phone number " + name + " not found")
                    .userMessage("Customer not found");
        }

        Customers customer = optionalCustomer.get();

        Specification<SalesOrders> spec = Specification
                .where(SalesOrdersSpecification.isNotDeleted())
                .and(SalesOrdersSpecification.searchParamMatches(searchParam))
                .and(SalesOrdersSpecification.hasSalesOrderNumber(salesOrderNumber))
                .and(SalesOrdersSpecification.hasWarehouse(warehouseId))
                .and(SalesOrdersSpecification.hasOrganizationRegion(organizationRegionId))
                .and(SalesOrdersSpecification.hasSalesOrderStatus(salesOrderStatus))
                .and(SalesOrdersSpecification.hasPaymentStatus(paymentStatus))
                .and(SalesOrdersSpecification.isCredit(isCredit))
                .and(SalesOrdersSpecification.hasInvoiceNumber(invoiceNumber))
                .and(SalesOrdersSpecification.hasStringDateFromTo(startDate, endDate))
                .and(SalesOrdersSpecification.hasCustomer(customer.getPublicId()));

        return salesOrdersRepository.findAll(spec, pageable);
    }

    @Override
    public SalesOrders updateNewCustomerOrders(Customers customer,
                                               List<ItemRequest> itemRequests,
                                               RoutePlanning routePlanning,
                                               UUID productReconPublicId) {

        SalesOrders salesOrder = new SalesOrders();
        OrganizationRegions organizationRegion = customer.getOrganizationRegions();
        Optional<Warehouse> warehouseOptional = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(routePlanning.getWarehouseId()));
        warehouseOptional.ifPresent(salesOrder::setWarehouse);
        salesOrder.setOrganizationRegions(organizationRegion);
        salesOrder.setCustomer(customer);
        salesOrder.setSalesOrderNumber("ORD" + counterService.getNextCounter(CounterType.SALES_ORDER));
        salesOrder.setBookedBy(routePlanning.getDeliveryAgentName());
        salesOrder.setExpectedDeliveryDate(LocalDate.now().plusDays(3));
        salesOrder.setPaymentStatus(PaymentStatusEnum.PENDING);
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.DELIVERED);
        salesOrder.setSoldAt(LocalDateTime.now());
        salesOrder.setSourceChannel(SourceChannelEnum.DELIVERY_AGENT_APP);
        salesOrder.setSalesOrderItems(new HashSet<>());
        salesOrder.setCreatedBy(customer.getCreatedBy());

        SalesOrders saved = salesOrdersRepository.save(salesOrder);

        for (ItemRequest itemsRequest : itemRequests) {
            Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(itemsRequest.getProductItemPublicId());
            if (productOptional.isPresent()) {
                SalesOrderItems salesOrderItem = SalesOrderItems.builder()
                        .productId(itemsRequest.getProductItemPublicId())
                        .quantity(itemsRequest.getQuantitySold())
                        .pricePerUnit(productOptional.get().getSellPrice())
                        .totalAmount(productOptional.get().getSellPrice().multiply(new BigDecimal(itemsRequest.getQuantitySold())))
                        .product(productOptional.get())
                        .productBeforePriceAfterTax(productOptional.get().getSellingPriceBeforeTax())
                        .build();
                salesOrderItem.setCreatedBy(customer.getCreatedBy());
                salesOrderItem.setStatus(OrderItemStatus.Delivered);
                salesOrderItem.setQuantityDelivered(itemsRequest.getQuantitySold());
                saved.addSalesOrderItem(salesOrderItem);
                salesOrderItem.setSalesOrder(saved);
                salesOrderItem.setSalesOrderPublicId(saved.getPublicId());
                salesOrderItemsRepository.save(salesOrderItem);
            }
            saved.setTotalAmount(saved
                    .getSalesOrderItems()
                    .stream()
                    .map(SalesOrderItems::getTotalPayable)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            saved.calculateTotalTax();
        }
        SalesOrders order = salesOrdersRepository.save(saved);

        routePlanning.addSalesOrder(order);
        routePlanningRepository.save(routePlanning);
        return order;
    }

    @Override
    public void cancelSalesOrder(UUID salesOrderPublicId, Boolean customerInitiated, UUID rejectionReasonPublicId, String name) {

        Person person;
        Customers customer;

        if (customerInitiated) {
            Optional<Customers> optionalCustomer = customersRepository.findByPhoneNumberAndDeletedIsFalse(name);

            if (optionalCustomer.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Customer with phone number " + name + " not found")
                        .userMessage("Customer not found");
            }

            customer = optionalCustomer.get();
            person = null;
        } else {
            Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
            if (optionalPerson.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("User with email " + name + " not found")
                        .userMessage("User not found");
            }
            person = optionalPerson.get();
            customer = null;
        }
        Optional<SalesOrders> salesOrderOptional = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrderPublicId);

        if (salesOrderOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order with public ID " + salesOrderPublicId + " not found")
                    .userMessage("Sales order not found");
        }

        SalesOrders salesOrder = salesOrderOptional.get();


        if (rejectionReasonPublicId != null) {
            Optional<RejectionsCode> optionalRejectionReason = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(rejectionReasonPublicId);
            if (optionalRejectionReason.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Rejection reason with public ID " + rejectionReasonPublicId + " not found")
                        .userMessage("Rejection reason not found");
            }

            RejectionsCode rejectionReason = optionalRejectionReason.get();

            if (rejectionReason.getModule() != Module.Customer) {
                throw new BadRequestRestApiException()
                        .developerMessage("Rejection reason with public ID " + rejectionReasonPublicId + " is not valid for customer module")
                        .userMessage("Invalid rejection reason");
            }

            salesOrder.setRejectionReason(rejectionReason);

        }

        if (customerInitiated) {
            if (!salesOrder.getCustomer().getPublicId().equals(customer.getPublicId())) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales order with public ID " + salesOrderPublicId + " does not belong to customer")
                        .userMessage("Sales order not found");
            }

            if (salesOrder.getSalesOrderStatus() != LoadingStatusEnum.ORDER_PLACED) {
                throw new BadRequestRestApiException()
                        .developerMessage("Customer cannot cancel sales order with public ID " + salesOrderPublicId + " at this stage")
                        .userMessage("Sorry!.Sales order cannot be cancelled at this stage,please contact support");
            }
        } else {

            if (!(salesOrder.getSalesOrderStatus() == LoadingStatusEnum.ORDER_PLACED || salesOrder.getSalesOrderStatus() == LoadingStatusEnum.PLANNING)) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales order with public ID " + salesOrderPublicId + " cannot be cancelled")
                        .userMessage("Sales order cannot be cancelled at this stage");
            }
        }

        salesOrder.setSalesOrderStatus(LoadingStatusEnum.CANCELLED);
        salesOrder.setPaymentStatus(PaymentStatusEnum.NOT_PAID);
        salesOrder.setUpdatedBy(customerInitiated ? null : person);
        salesOrder.setUpdateDate(LocalDateTime.now());
        SalesOrders saved = salesOrdersRepository.save(salesOrder);

        Map<Product, Integer> reservedStockMap = new HashMap<>();

        saved.getSalesOrderItems()
                .forEach(salesOrderItem -> {

                    Product product = salesOrderItem.getProduct();
                    Integer quantity = salesOrderItem.getQuantity();
                    reservedStockMap.put(product, quantity);
                    salesOrderItem.setUpdateDate(LocalDateTime.now());
                    salesOrderItem.setUpdatedBy(personRepository.findByEmailAndDeletedIsFalse(name).orElse(null));
                    salesOrderItemsRepository.save(salesOrderItem);
                });

        stockUpdateHelper.updateCancelledSalesOrderBookedStock(reservedStockMap, saved.getWarehouse(), InventoryEvent.CANCEL_ORDER.name());

        String description = "Sales order " + saved.getSalesOrderNumber() + " cancelled by " +
                (customerInitiated ? "customer" : person.getFullName());
        eventsRepository.save(new Events(saved.getPublicId().toString(),
                "Order cancelled",
                LocalDateTime.now(),
                description,
                "CANCELLED",
                customerInitiated ? null : person));

        reservedStockMap.forEach((product, quantity) -> {
            stockMovementHelper.recordStockMovement(product,
                    quantity,
                    StockMovementTransactionType.ORDER_CANCELLATION,
                    TransactionBoundType.INCOMING,
                    customerInitiated ? saved.getCustomer().getName() : person.getFullName(),
                    saved.getWarehouse());
        });

        if (saved.isCreditApplied()) {
            bnplHelper.cancelCreditItemsForOrder(salesOrder, name);
        }
    }

    @Override
    public void requestForSpecialDiscount(SpecialRequest specialRequest, String name) {

        if (Validators.anyEqualNull(specialRequest.getCustomerId(), specialRequest.getItems())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer ID and Items must be provided")
                    .userMessage("Customer ID and Items must be provided");
        }

        if (specialRequest.getItems().stream().anyMatch(item -> item.getQuantity() <= 0)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Quantity must be greater than zero")
                    .userMessage("Quantity must be greater than zero");
        }

        if (specialRequest.getItems().stream().anyMatch(item -> item.getTotalDiscountRequest() == null)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Total discount request must be provided")
                    .userMessage("Total discount request must be provided");
        }

        if (specialRequest.getItems().stream().anyMatch(item -> item.getProductPublicId() == null ||
                item.getProductName() == null || item.getUnitPrice() == null)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product ID, Product Name and Unit Price must be provided for each item")
                    .userMessage("Product ID, Product Name and Unit Price must be provided for each item");
        }

        String requestedBy = specialRequest.getHasDeliveryAgent() ? specialRequest.getDeliveryAgent() : "";
        String requestedByPublicId = specialRequest.getHasDeliveryAgent() ? specialRequest.getDeliveryAgentPublicId() : "";

        if (!specialRequest.getHasDeliveryAgent()) {
            Optional<SalesAgents> optionalSalesAgent = salesAgentsRepository.findSalesAgentsByPhoneNumberAndDeletedIsFalse(name);

            if (optionalSalesAgent.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales agent with phone number " + name + " not found")
                        .userMessage("Sales agent not found");
            }
            SalesAgents salesAgent = optionalSalesAgent.get();

            requestedBy = salesAgent.getName();
            requestedByPublicId = salesAgent.getPublicId().toString();
        }

        Optional<Customers> optionalCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(specialRequest.getCustomerId());
        if (optionalCustomer.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with public ID " + specialRequest.getCustomerId() + " not found")
                    .userMessage("Customer not found");
        }

        Customers customer = optionalCustomer.get();

        if (customer.getStatus() == CustomerStatus.De_Activated) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer is De_Activated")
                    .userMessage("Sorry!. Customer is De_Activated");
        }


        List<RequestItems> requestItems = new ArrayList<>();

        if (specialRequest.getItems() != null && !specialRequest.getItems().isEmpty()) {

            specialRequest.getItems().forEach(requestItem -> {
                Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(requestItem.getProductPublicId());
                if (optionalProduct.isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Product with ID " + requestItem.getProductPublicId() + " not found")
                            .userMessage("One of the products in the discount approval request was not found. Please check the product IDs.");
                }
            });


            for (RequestItemDTO itemDTO : specialRequest.getItems()) {
                RequestItems requestItem = new RequestItems();
                requestItem.setProductId(itemDTO.getProductPublicId());
                requestItem.setProductName(itemDTO.getProductName());
                requestItem.setQuantity((int) itemDTO.getQuantity());
                requestItem.setUnitPrice(itemDTO.getUnitPrice());
                requestItem.setDiscountRequest(itemDTO.getTotalDiscountRequest());
                requestItem.setCustomerPublicId(specialRequest.getCustomerId().toString());
                requestItem.setProductItemPublicId(itemDTO.getProductItemPublicId());

                RequestItems saved = requestItemRepository.save(requestItem);
                requestItems.add(saved);
            }
        }

        // check if there is an existing pending discount approval for the same customer and sales agent
        Optional<DiscountApproval> existingApproval = discountApprovalRepository.findByCustomerPublicIdAndRequestedByPublicIdAndApprovalStatusAndDeletedIsFalse(
                customer.getPublicId().toString(),
                requestedByPublicId,
                DiscountApprovalStatus.Pending);


        if (existingApproval.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("There is already a pending discount approval request for this customer by the same sales agent")
                    .userMessage("A pending discount approval request already exists. Please wait for it to be processed before submitting a new request.");
        }

        DiscountApproval discountApproval = new DiscountApproval();
        discountApproval.setApprovalStatus(DiscountApprovalStatus.Pending);
        discountApproval.setRequestedBy(requestedBy);
        discountApproval.setRequestedByPublicId(requestedByPublicId);
        discountApproval.setCustomer(customer.getName());
        discountApproval.setCustomerPublicId(customer.getPublicId().toString());
        discountApproval.setCreationTime(LocalDateTime.now());
        discountApproval.setRequestItems(requestItems);
        discountApproval.setHasVanSales(specialRequest.getHasDeliveryAgent());
        DiscountApproval savedDiscountApproval = discountApprovalRepository.save(discountApproval);

        String description = String.format("Sales agent %s requested a special discount for customer %s with total discount request of %s",
                requestedBy,
                customer.getName(),
                specialRequest.getItems().stream()
                        .map(RequestItemDTO::getTotalDiscountRequest)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        eventsRepository.save(new Events(savedDiscountApproval.getPublicId().toString(),
                "DiscountApproval",
                LocalDateTime.now(),
                description,
                "Discount Special Request",
                null));

        customer.setSpecialDiscountRequested(true);
        customersRepository.save(customer);
    }

    @Override
    public BigDecimal checkForDiscountApproval(CartDiscountRequest cartDiscountRequest, String name) {

        return discountHelper.calculateCartDiscount(cartDiscountRequest, name);
    }

    @Override
    public void recalculateDiscount(UUID salesOrderPublicId, String name) {

        SalesOrders salesOrder = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(salesOrderPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sales order with public ID " + salesOrderPublicId + " not found")
                        .userMessage("Sales order not found"));

        if (salesOrder.isHasSpecialDiscount()) {
            log.info("Sales order {} has special discount. Skipping volume discount recalculation.", salesOrder.getSalesOrderNumber());

        } else {
            discountHelper.recalculateVolumeDiscount(salesOrder);
            log.info("Recalculating volume discount for sales order {}", salesOrder.getSalesOrderNumber());

        }
    }

    @Override
    public Page<SalesOrderAgent> getSalesOrderAgent(Pageable pageable) {
        return salesOrderAgentRepository.findAll(pageable);
    }

    @Override
    public Page<SalesSummaryDto> fetchSalesAgentOderSummary(UUID organisationRegionPublicId, UUID organisationLocationPublicId, LocalDate startDate, LocalDate endDate, String searchParam, Principal principal, Pageable pageable) {
        List<SalesSummaryDto> salesSummaryDtoList = new ArrayList<>();
        List<Customers> allCustomers = customersRepository.findByDeletedIsFalse(pageable).getContent();

        allCustomers.forEach(customer -> {
            log.info("Fetching customer {}, {}", customer.getPublicId(), customer.getName());
            List<SalesOrders> salesOrdersList = salesOrdersRepository.findByCustomerAndSalesOrderStatusInAndDeletedIsFalse(customer, List.of(LoadingStatusEnum.DELIVERED));
            double avgTotalAmount = salesOrdersList.stream()
                    .map(SalesOrders::getTotalAmount)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average()
                    .orElse(0.0);

            int orderSize = salesOrdersList.size();
            SalesSummaryDto salesSummaryDto = SalesSummaryDto.builder()
                    .customer(customer.getName())
                    .orderSize(orderSize)
                    .averageTotalAmount(Math.round(avgTotalAmount))
                    .build();
            salesSummaryDtoList.add(salesSummaryDto);
        });

        return new PageImpl<>(salesSummaryDtoList, pageable, salesSummaryDtoList.size());
    }

    @Override
    public List<ZoneAverageDTO1> productPerformanceDateRangeByZone(
            UUID productId, UUID zoneId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable,
            Principal principal) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ZoneAverageDTO1> cq = cb.createQuery(ZoneAverageDTO1.class);
        Root<SalesOrders> order = cq.from(SalesOrders.class);
        Join<SalesOrders, Customers> customer = order.join("customer");
        Join<SalesOrders, SalesOrderItems> items = order.join("salesOrderItems");
        Join<SalesOrderItems, Product> product = items.join("product");

        Path<LocalDate> deliveryDate = order.get("expectedDeliveryDate");

        Expression<Integer> avgQuantity = cb.toInteger(cb.avg(cb.coalesce(items.get("quantity"), 0)));

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(order.get("salesOrderStatus"), "DELIVERED"));
        if (zoneId != null) predicates.add(cb.equal(customer.get("organizationLocation").get("publicId"), zoneId));
        if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(order.get("expectedDeliveryDate"), startDate));
        if (endDate != null) predicates.add(cb.lessThanOrEqualTo(order.get("expectedDeliveryDate"), endDate));
        if (productId != null) predicates.add(cb.equal(product.get("publicId"), productId));

        cq.multiselect(deliveryDate, avgQuantity)
                .where(predicates.toArray(new Predicate[0]))
                .groupBy(deliveryDate);

        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public List<ZoneNameDateAverageProjection> productPerformance(
            UUID productId,
            UUID regionId,
            LocalDate startDate,
            LocalDate endDate,
            UUID locationId,
            Pageable pageable,
            Principal principal) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(principal.getName());

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("Sorry!. user not found");
        }

        Person person = optionalPerson.get();

        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("no associated supplier")
                    .userMessage("Sorry!. no associated supplier");
        }

        Optional<Suppliers> suppliersOptional = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()));

        if (suppliersOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("supplier not found")
                    .userMessage("Sorry! Supplier not found");
        }

        Suppliers supplier = suppliersOptional.get();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<SalesOrders> order = cq.from(SalesOrders.class);
        Join<SalesOrders, Customers> customer = order.join("customer");
        Join<SalesOrders, SalesOrderItems> items = order.join("salesOrderItems");
        Join<SalesOrderItems, Product> product = items.join("product");
        Join<Product, Suppliers> productSuppliersJoin = product.join("suppliers");

        Expression<Double> totalAmountExpr = cb.toDouble(
                cb.sum(
                        cb.prod(
                                cb.coalesce(product.<Double>get("buyingPrice"), 0.0),
                                cb.coalesce(items.<Integer>get("quantity"), 0)
                        )
                )
        );


        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(order.get("salesOrderStatus"), "DELIVERED"));
        predicates.add(cb.equal(productSuppliersJoin.get("publicId"), supplier.getPublicId()));

        if (regionId != null) {
            predicates.add(customer.get("organizationRegions").get("publicId").in(regionId));
        }

        if (locationId != null) {
            predicates.add(customer.get("organizationLocation").get("publicId").in(locationId));
        }

        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(order.get("creationTime"), startDate.atStartOfDay()));
        }

        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(order.get("creationTime"), endDate.atTime(LocalTime.MAX)));
        }

        if (productId != null) {
            predicates.add(cb.equal(product.get("publicId"), productId));
        }

        cq.multiselect(
                        order.get("creationTime").alias("creationTime"),
                        totalAmountExpr.alias("totalAmount")
                )
                .where(predicates.toArray(new Predicate[0]))
                .groupBy(order.get("creationTime"));

        List<Tuple> tuples = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return tuples.stream()
                .map(t -> new ZoneNameDateAverageProjection(
                        ((LocalDateTime) t.get("creationTime")).toLocalDate(),
                        (Double) t.get("totalAmount")
                ))
                .collect(Collectors.toList());
    }


    @Override
    public Page<ZoneNameAverageProjection> orderValuePerZone(
            UUID productId,
            UUID regionId,
            LocalDate startDate,
            LocalDate endDate,
            UUID locationId,
            Pageable pageable,
            Principal principal
    ) {
        // Validate person
        Person person = personRepository.findByEmailAndDeletedIsFalse(principal.getName())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("user not found")
                        .userMessage("Sorry!. user not found"));

        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("no associated supplier")
                    .userMessage("Sorry!. no associated supplier");
        }

        Suppliers supplier = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<SalesOrders> order = cq.from(SalesOrders.class);
        Join<SalesOrders, Customers> customer = order.join("customer");
        Join<SalesOrders, SalesOrderItems> items = order.join("salesOrderItems");
        Join<SalesOrderItems, Product> product = items.join("product");
        Join<Product, Suppliers> productSuppliersJoin = product.join("suppliers");

        // total amount = buyingPrice * quantityDelivered
        Expression<Double> sumTotalAmountExpr = cb.toDouble(
                cb.sum(
                        cb.prod(
                                cb.coalesce(product.<Double>get("buyingPrice"), 0.0),
                                cb.coalesce(items.<Integer>get("quantityDelivered"), 0)
                        )
                )
        );

        Path<String> locationNamePath = customer.get("organizationLocation").get("name");

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(order.get("salesOrderStatus"), "DELIVERED"));
        predicates.add(cb.equal(productSuppliersJoin.get("publicId"), supplier.getPublicId()));

        if (regionId != null) {
            predicates.add(cb.equal(customer.get("organizationRegions").get("publicId"), regionId));
        }
        if (locationId != null) {
            predicates.add(cb.equal(customer.get("organizationLocation").get("publicId"), locationId));
        }
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(order.get("creationTime"), startDate.atStartOfDay()));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(order.get("creationTime"), endDate.atTime(LocalTime.MAX)));
        }
        if (productId != null) {
            predicates.add(cb.equal(product.get("publicId"), productId));
        }

        cq.multiselect(
                        sumTotalAmountExpr.alias("totalAmount"),
                        locationNamePath.alias("locationName")
                )
                .where(predicates.toArray(new Predicate[0]))
                .groupBy(locationNamePath);

        List<Tuple> tuples = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        List<ZoneNameAverageProjection> content = tuples.stream()
                .map(t -> new ZoneNameAverageProjection(
                        (Double) t.get("totalAmount"),
                        (String) t.get("locationName")
                ))
                .collect(Collectors.toList());

        // count query (distinct locations)
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<SalesOrders> countRoot = countQuery.from(SalesOrders.class);
        Join<SalesOrders, Customers> countCustomer = countRoot.join("customer");
        Join<SalesOrders, SalesOrderItems> countItems = countRoot.join("salesOrderItems");
        Join<SalesOrderItems, Product> countProduct = countItems.join("product");
        Join<Product, Suppliers> countProductSuppliersJoin = countProduct.join("suppliers");

        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.equal(countRoot.get("salesOrderStatus"), "DELIVERED"));
        countPredicates.add(cb.equal(countProductSuppliersJoin.get("publicId"), supplier.getPublicId()));

        if (regionId != null) {
            countPredicates.add(cb.equal(countCustomer.get("organizationRegions").get("publicId"), regionId));
        }
        if (locationId != null) {
            countPredicates.add(cb.equal(countCustomer.get("organizationLocation").get("publicId"), locationId));
        }
        if (startDate != null) {
            countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("creationTime"), startDate.atStartOfDay()));
        }
        if (endDate != null) {
            countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("creationTime"), endDate.atTime(LocalTime.MAX)));
        }
        if (productId != null) {
            countPredicates.add(cb.equal(countProduct.get("publicId"), productId));
        }

        countQuery.select(cb.countDistinct(countCustomer.get("organizationLocation").get("name")))
                .where(countPredicates.toArray(new Predicate[0]));

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public ByteArrayOutputStream generateInvoice(UUID salesOrderPublic, LocalDate startDate, LocalDate endDate, Pageable pageable, Principal principal) {
        SalesOrders salesOrder = getSalesOrder(salesOrderPublic);
        return invoiceReceipt.createReceipt(salesOrder, principal);
    }

    @Override
    public ByteArrayOutputStream generateCustomerInvoiceReceipt(UUID salesOrderPublic, OrderType orderType,
                                                                String name, String orderNumber) {
        return traInvoiceReceipt.generateCustomerInvoiceReceipt(salesOrderPublic, orderType, name, orderNumber);
    }

    @Override
    public OrderStatsDTO getSalesOrderStats(String searchParam, String salesOrderNumber, UUID customerId, UUID warehouseId, UUID organizationRegionId, SourceChannelEnum sourceChannel, LoadingStatusEnum salesOrderStatus, PaymentStatusEnum paymentStatus, String invoiceNumber,
                                            LocalDate startDate, LocalDate endDate, UUID locationId, Boolean bookedBySalesAgent, UUID salesAgentPublicId, UUID deliveryAgentPublicId,
                                            UUID parentCustomerPublicId, Boolean isCredit, List<LoadingStatusEnum> statuses, Boolean isWalkIn, Boolean bookedByParent) {
        SalesOrdersPageableDTO salesOrdersPageableDTO = getAllSalesOrders(searchParam,
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
                Pageable.unpaged()
        );

        long totalOrders = salesOrdersPageableDTO.getTotalElements();
        BigDecimal totalRevenue = salesOrdersPageableDTO.getContent().stream()
                .map(SalesOrders::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalQuantityDelivered = salesOrdersPageableDTO.getContent().stream()
                .filter(Objects::nonNull)
                .flatMap(order -> Optional.ofNullable(order.getSalesOrderItems()).orElseGet(Set::of).stream())
                .mapToLong(item -> item.getQuantityDelivered() > 0 ? item.getQuantityDelivered() : item.getQuantity())
                .sum();

        return new OrderStatsDTO(totalOrders, totalRevenue, totalQuantityDelivered);
    }

    @Override
    public void generateTraReceipt(UUID salesOrderPublicId, String name) {
        SalesOrders salesOrder = getSalesOrder(salesOrderPublicId);

        if (salesOrder.getEnterpriseOrder() != null && salesOrder.getEnterpriseOrder()) {
            log.info("TRA receipt cannot be generated for enterprise orders. Current order status: {}", salesOrder.getSalesOrderStatus());
            throw new BadRequestRestApiException()
                    .developerMessage("TRA receipt cannot be generated for enterprise orders. Current order status: " + salesOrder.getSalesOrderStatus())
                    .userMessage("TRA receipt cannot be generated for enterprise orders. Current order status: " + salesOrder.getSalesOrderStatus());
        }

        if (salesOrder.getSalesOrderStatus() != LoadingStatusEnum.DELIVERED) {
            log.info("TRA receipt can only be generated for delivered orders. Current order status: {}", salesOrder.getSalesOrderStatus());
            throw new BadRequestRestApiException()
                    .developerMessage("TRA receipt can only be generated for delivered orders. Current order status: " + salesOrder.getSalesOrderStatus())
                    .userMessage("TRA receipt can only be generated for delivered orders. Current order status: " + salesOrder.getSalesOrderStatus());
        }

        if (salesOrder.getTraReceiptGenerated()) {
            throw new BadRequestRestApiException()
                    .developerMessage("TRA receipt has already been generated for this order.")
                    .userMessage("TRA receipt has already been generated for this order.");
        }

        if (salesOrder.getCustomer().getTin() != null && !salesOrder.getCustomer().getTin().isBlank()) {
            log.info("Customer {} has TIN {}, proceeding with TRA receipt generation.", salesOrder.getCustomer().getName(), salesOrder.getCustomer().getTin());
            if (!customerCheckerHelper.validateCustomerTinNumber(salesOrder.getCustomer())) {
                log.info("Customer {} does not have a valid TIN number. Skipping TRA receipt generation.", salesOrder.getCustomer().getName());
                throw new BadRequestRestApiException()
                        .developerMessage("Customer does not have a valid TIN number. TRA receipt cannot be generated.")
                        .userMessage("Customer does not have a valid TIN number. TRA receipt cannot be generated.");
            }
        }

        log.info("==============order state is {}, tra receipt generated is {}==============", salesOrder.getSalesOrderStatus(), salesOrder.getTraReceiptGenerated());


        this.generateTraCustomerReceipt(salesOrder);
    }

    @Override
    public Page<SalesOrders> getAllCustomerSalesOrders(UUID customerPublicId, Pageable pageable) {
        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId).orElse(null);
        if (customer == null) {
            return new PageImpl<>(Collections.EMPTY_LIST, pageable, 0);
        } else {
            List<SalesOrders> ordersList = salesOrdersRepository.findTop5ByCustomerAndDeletedFalseOrderByCreationTimeDesc(customer);
            return new PageImpl<>(ordersList, pageable, ordersList.size());
        }
    }

    @Override
    public void notifyCustomerWithSmsOrderInPlanningAndTransit() throws IOException, InterruptedException {
        notifyCustomers(
                salesOrdersRepository.findBySmsNotifyInPlanningIsFalseAndSalesOrderStatusAndDeletedIsFalse(LoadingStatusEnum.PLANNING),
                "%s, Oda yako imepokelewa na ipo katika maandalizi kwa ajili ya usafirishaji. Tarajia kupokea oda yako siku ya kesho. Asante kwa kuchagua Itibari",
                true, false
        );

        notifyCustomers(
                salesOrdersRepository.findBySmsNotifyInTransitIsFalseAndSalesOrderStatusAndDeletedIsFalse(LoadingStatusEnum.IN_TRANSIT),
                "%s, Oda yako ipo njiani na mhudumu wetu wa usafirishaji atafika dukani kwako leo. Asante kwa kuichagua Itibari.",
                false, true
        );
    }

    @Override
    public SalesOrders createWalkInSalesOrder(WalkInSalesOrdersRequest walkInSalesOrdersRequest, String userName) {
        customerCheckerHelper.checkIfCustomerHasActiveOrder(walkInSalesOrdersRequest.customerId());

        Person person = validateLoggedInUser(userName);

        Customers customer = getCustomer(walkInSalesOrdersRequest.customerId());

        boolean hasValidTin = customerCheckerHelper.validateCustomerTinNumber(customer);

        Map<UUID, OrderProductInfo> productSkuMap = gatherProductItems(walkInSalesOrdersRequest.orderItemsRequest());

        OrganizationRegions organizationRegion = customer.getOrganizationRegions();

        Warehouse warehouse = getWarehouseByRegion(organizationRegion);

        validateProductIdsAndStockAvailability(productSkuMap, warehouse);

        Integer nextCounter = counterService.getNextCounter(CounterType.SALES_ORDER);

        SalesOrders salesOrder = new SalesOrders();
        salesOrder.setOrganizationRegions(organizationRegion);
        salesOrder.setWarehouse(warehouse);
        salesOrder.setSourceChannel(getSourceChannel(walkInSalesOrdersRequest.sourceChannel()));
        salesOrder.setBookedBy(person.getFullName());
        if (customer.getServedBy() != null) {
            salesOrder.setSalesAgentPublicId(String.valueOf(customer.getServedBy().getPublicId()));
        }
        salesOrder.setCustomer(customer);
        salesOrder.setSalesOrderNumber(String.format("ORD%d", nextCounter));
        salesOrder.setExpectedDeliveryDate(LocalDate.now().plusDays(3));
        salesOrder.setPaymentStatus(PaymentStatusEnum.PAID);
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.DELIVERED);
        salesOrder.setSalesOrderItems(new HashSet<>());
        salesOrder.setCreatedBy(person);
        salesOrder.setBookedBySalesAgent(salesOrder.isBookedBySalesAgent());
        salesOrder.setCreationTime(LocalDateTime.now());
        salesOrder.setOrganizationRegions(organizationRegion);
        salesOrder.setHasValidTin(hasValidTin);
        salesOrder.setWalkIn(true);

        SalesOrders savedOrder = salesOrdersRepository.save(salesOrder);

        productSkuMap.forEach((productId, orderProductInfo) -> {
            Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(productId);

            if (productOptional.isPresent()) {
                Product product = productOptional.get();

                BigDecimal taxAmount = Optional.ofNullable(product.getTaxAmount()).orElse(BigDecimal.ZERO);
                BigDecimal sellPrice = Optional.ofNullable(product.getSellPrice()).orElse(BigDecimal.ZERO);

                BigDecimal taxCodeAmount = taxAmount.multiply(BigDecimal.valueOf(orderProductInfo.getQuantity()));
                BigDecimal totalAmount = sellPrice.multiply(BigDecimal.valueOf(orderProductInfo.getQuantity()));

                BigDecimal taxPercentage = product.getTaxCode() != null ? product.getTaxCode().getCodeValue() : BigDecimal.ZERO;

                BigDecimal totalDiscount = orderProductInfo.getSpecialDiscount().add(orderProductInfo.getVolumeDiscount());

                BigDecimal totalAmountLessDiscount = totalAmount.subtract(totalDiscount);

                SalesOrderItems salesOrderItem = SalesOrderItems.builder()
                        .productId(productId)
                        .quantity(orderProductInfo.getQuantity())
                        .pricePerUnit(sellPrice)
                        .status(OrderItemStatus.Delivered)
                        .quantityDelivered(orderProductInfo.getQuantity())
                        .totalAmount(totalAmountLessDiscount)
                        .product(product)
                        .taxAmountValue(taxCodeAmount)
                        .productBeforePriceAfterTax(product.getSellingPriceBeforeTax())
                        .taxPercentageApplied(taxPercentage)
                        .deliveredDate(nairobiDate)
                        .discount(totalDiscount)
                        .hasDiscount(orderProductInfo.isHasDiscount())
                        .walkInVolumeDiscount(orderProductInfo.getVolumeDiscount())
                        .walkInSpecialDiscount(orderProductInfo.getSpecialDiscount())
                        .taxApplied(taxPercentage.compareTo(BigDecimal.ZERO) > 0)
                        .build();

                salesOrderItem.setSalesOrder(savedOrder);
                salesOrderItem.setSalesOrderPublicId(savedOrder.getPublicId());
                salesOrderItem.setSalesAgentPublicId(salesOrder.getSalesAgentPublicId());

                SalesOrderItems savedSalesOrderItem = salesOrderItemsRepository.save(salesOrderItem);
                updateLiveStock(savedOrder.getWarehouse(),
                        product,
                        savedSalesOrderItem.getQuantityDelivered());

                savedOrder.addSalesOrderItem(savedSalesOrderItem);
            }
        });

        savedOrder.setTotalAmount(savedOrder.getSalesOrderItems().stream().map(item -> Optional.ofNullable(item.getTotalAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        savedOrder.calculateTotalTax();

        BigDecimal reduceAmount = savedOrder.getSalesOrderItems().stream()
                .map(item -> Optional.ofNullable(item.getTaxAmountValue()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        savedOrder.setTaxAmountValue(reduceAmount);

        savedOrder.setExpectedDeliveryDate(LocalDate.now());
        salesOrder.setSoldAt(LocalDateTime.now());
        SalesOrders savedSalesOrder = salesOrdersRepository.save(savedOrder);

        Map<Product, Integer> reservedStockMap = new HashMap<>();
        savedSalesOrder.getSalesOrderItems()
                .forEach(salesOrderItem -> {
                    Product product = salesOrderItem.getProduct();
                    Integer quantity = salesOrderItem.getQuantity();
                    reservedStockMap.put(product, quantity);
                });

        stockUpdateHelper.updateSalesOrderBookedStock(reservedStockMap, savedSalesOrder.getWarehouse(),
                InventoryEvent.CREATE_ORDER.name());


        reservedStockMap.forEach((product, quantity) -> {
            stockMovementHelper.recordStockMovement(product,
                    quantity,
                    StockMovementTransactionType.ORDER_BOOKING,
                    TransactionBoundType.OUTGOING,
                    savedSalesOrder.getBookedBy(),
                    savedSalesOrder.getWarehouse());
        });

        SalesOrders finalSavedOrder = salesOrdersRepository.findByPublicId(salesOrder.getPublicId());

        ProductRecon productRecon = createWalkInProductReconciliation(finalSavedOrder);

        createWalkInCashReconciliation(productRecon);

        String description = String.format("Sales order %s created with total amount %.2f, order type %s, credit applied %s, has valid Tin %s, has special discount %s",
                finalSavedOrder.getSalesOrderNumber(),
                finalSavedOrder.getTotalAmountPayable(),
                finalSavedOrder.getOrderType(),
                finalSavedOrder.isCreditApplied(),
                finalSavedOrder.isHasValidTin(),
                finalSavedOrder.isHasSpecialDiscount());

        eventsRepository.save(new Events(savedSalesOrder.getPublicId().toString(),
                "Order Created",
                LocalDateTime.now(),
                description,
                "CREATED",
                person));


        this.generateTraCustomerReceipt(salesOrder);

        return salesOrdersRepository.save(finalSavedOrder);
    }

    @Override
    public Page<ProductRecon> getWalkInProductReconciliations(String searchParam, Boolean reconciled, String warehousePublicId, ReconStatus status, UUID createdBy, LocalDate startDate, LocalDate endDate, Principal principal, Pageable pageable) {

        LocalDate localDateTimeStart = startDate != null ? startDate : LocalDate.now();
        LocalDate localDateTimeEnd = startDate != null ? startDate : LocalDate.now();

        Specification<ProductRecon> specification = Specification.where(
                        ProductReconSpecification.hasReconciled(reconciled))
                .and(ProductReconSpecification.hasStatus(status))
                .and(ProductReconSpecification.hasCreatedBy(createdBy))
                .and(ProductReconSpecification.hasWarehousePublicId(warehousePublicId))
                .and(ProductReconSpecification.hasDateBetween(localDateTimeStart.toString(), localDateTimeEnd.toString()))
                .and(ProductReconSpecification.isNotDeleted())
                .and(ProductReconSpecification.hasSearchParam(searchParam))
                .and(ProductReconSpecification.isWalkIn(true));
        return productReconRepository.findAll(specification, pageable);
    }


    private ProductRecon createWalkInProductReconciliation(SalesOrders finalSavedOrder) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");

        LocalDateTime startOfDay = LocalDate.now(nairobiZone).atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        ProductRecon productRecon = productReconRepository.findByCreationTimeBetweenAndWalkInIsTrueAndDeletedIsFalse(startOfDay, endOfDay).orElse(null);

        if (productRecon == null) {

            productRecon = new ProductRecon();
            productRecon.setWarehouseName(finalSavedOrder.getWarehouse().getWarehouseName());
            productRecon.setWarehousePublicId(String.valueOf(finalSavedOrder.getWarehouse().getPublicId()));
            productRecon.setProductItems(new ArrayList<>());
            productRecon.setCreatedBy(finalSavedOrder.getCreatedBy());
            productRecon.setStatus(ReconStatus.Reconciled);
            productRecon.setWalkIn(true);
            productRecon.setDeliveryId("WD-" + counterService.getNextCounter(CounterType.WALK_IN_DELIVERY_ID));
        } else {
            productRecon.setUpdatedBy(finalSavedOrder.getCreatedBy());
        }

        ProductRecon savedProductRecon = productReconRepository.save(productRecon);

        List<ProductItem> productItems = new ArrayList<>();

        if (!savedProductRecon.getProductItems().isEmpty()) {
            for (SalesOrderItems salesOrderItem : finalSavedOrder.getSalesOrderItems()) {
                ProductItem productItem = productItemRepository.findByProductReconAndProductPublicIdAndDeletedIsFalse(savedProductRecon, salesOrderItem.getProductId()).orElse(null);

                if (productItem == null) {
                    productItem = new ProductItem();
                    productItem.setProductRecon(savedProductRecon);
                    productItem.setProductName(salesOrderItem.getProduct().getProductName());
                    productItem.setProductPublicId(salesOrderItem.getProduct().getPublicId());
                    productItem.setQuantityPicked(salesOrderItem.getQuantityDelivered());
                    productItem.setProductCode(salesOrderItem.getProduct().getSku());
                    productItem.setProductSellingPrice(salesOrderItem.getPricePerUnit());
                    productItem.setHasVanSale(false);
                    productItem.setHasOrdered(true);  // this for?
                    productItem.setOrderQuantity(salesOrderItem.getQuantity());
                    productItem.setDiscount(salesOrderItem.getDiscount());
                    ProductItem savedProductItem = productItemRepository.save(productItem);
                    productItems.add(savedProductItem);
                } else {
                    long qtyDelivered = productItem.getQuantityPicked() + salesOrderItem.getQuantityDelivered();
                    BigDecimal ttDiscount =
                            Optional.ofNullable(productItem.getDiscount()).orElse(BigDecimal.ZERO)
                                    .add(Optional.ofNullable(salesOrderItem.getDiscount()).orElse(BigDecimal.ZERO));


                    productItem.setProductRecon(savedProductRecon);
                    productItem.setProductName(salesOrderItem.getProduct().getProductName());
                    productItem.setProductPublicId(salesOrderItem.getProduct().getPublicId());
                    productItem.setQuantityPicked(qtyDelivered);
                    productItem.setQuantitySold(qtyDelivered);
                    productItem.setProductCode(salesOrderItem.getProduct().getSku());
                    productItem.setProductSellingPrice(salesOrderItem.getPricePerUnit());
                    productItem.setHasVanSale(false);
                    productItem.setHasOrdered(true);  // this for?
                    productItem.setOrderQuantity(qtyDelivered);
                    productItem.setDiscount(ttDiscount);
                    ProductItem savedProductItem = productItemRepository.save(productItem);
                    productItems.add(savedProductItem);
                }
            }
        } else {
            for (SalesOrderItems salesOrderItem : finalSavedOrder.getSalesOrderItems()) {
                ProductItem productItem = new ProductItem();
                productItem.setProductRecon(savedProductRecon);
                productItem.setProductName(salesOrderItem.getProduct().getProductName());
                productItem.setProductPublicId(salesOrderItem.getProduct().getPublicId());
                productItem.setQuantityPicked(salesOrderItem.getQuantityDelivered());
                productItem.setQuantitySold(salesOrderItem.getQuantityDelivered());
                productItem.setProductCode(salesOrderItem.getProduct().getSku());
                productItem.setProductSellingPrice(salesOrderItem.getPricePerUnit());
                productItem.setHasVanSale(false);
                productItem.setHasOrdered(true);  // this for?
                productItem.setOrderQuantity(salesOrderItem.getQuantity());
                productItem.setDiscount(salesOrderItem.getDiscount());
                ProductItem savedProductItem = productItemRepository.save(productItem);
                productItems.add(savedProductItem);
            }
        }

        savedProductRecon.setProductItems(productItems);
        savedProductRecon.setReconciled(true);
        return productReconRepository.save(savedProductRecon);
    }

    private void updateLiveStock(Warehouse warehouse, Product product, int quantityDelivered) {

        liveStockHelper.updateLiveStockValue(warehouse.getPublicId(),
                product.getPublicId(),
                quantityDelivered,
                ProductUpdateType.ISSUED_STOCK,
                LocalDate.now());

        productStockHelper.updateProductStock(product, warehouse, (long) quantityDelivered,
                StockMovementTransactionType.ISSUANCE);

        liveStockHelper.updateLiveStockValue(warehouse.getPublicId(),
                product.getPublicId(),
                quantityDelivered,
                ProductUpdateType.SOLD_STOCK,
                LocalDate.now());
    }

    @Override
    public void transitionOrders(UUID orderPublicId, String name) {
        Person person = personRepository.findByEmailAndDeletedIsFalse(name).orElseThrow(
                () -> new BadRequestRestApiException()
                        .developerMessage("user not found")
                        .userMessage("user not found")
        );

        SalesOrders orders = getSalesOrder(orderPublicId);
        if (orders.getSalesOrderStatus() != LoadingStatusEnum.ORDER_PLACED) {
            throw new BadRequestRestApiException()
                    .developerMessage("Action appliers on order placed")
                    .userMessage("Sorry!. Action only on order placed");
        }
        orders.setSalesOrderStatus(LoadingStatusEnum.PLANNING);
        orders.setUpdateDate(LocalDateTime.now());
        orders.setUpdatedBy(person);

        salesOrdersRepository.save(orders);

    }

    private void notifyCustomers(List<SalesOrders> orders, String messageTemplate,
                                 boolean markPlanning, boolean markTransit) throws IOException, InterruptedException {
        if (orders == null || orders.isEmpty()) return;

        for (SalesOrders order : orders) {
            if (order.getCustomer() == null) continue;
            String name = order.getCustomer().getStoreName() != null
                    ? order.getCustomer().getStoreName()
                    : order.getCustomer().getName();

            String phone = order.getCustomer().getPhoneNumber();
            String fullName = order.getCustomer().getName();
            String orderNumber = order.getSalesOrderNumber();

            if (phone == null || phone.isBlank()) continue;

            String formattedMessage = String.format(messageTemplate, name);
//            tumaMessage.tumaUjumbeKwaMteja(phone, formattedMessage, fullName);

            if (markPlanning) {
                tumaMessage.whatsAppCustomer(LoadingStatusEnum.PLANNING.name(), phone, name, orderNumber);
            } else {
                tumaMessage.whatsAppCustomer(LoadingStatusEnum.IN_TRANSIT.name(), phone, name, orderNumber);
            }

            // Mark as notified
            if (markPlanning) order.setSmsNotifyInPlanning(true);
            if (markTransit) order.setSmsNotifyInTransit(true);
            salesOrdersRepository.save(order);
        }
    }


    private void generateTraCustomerReceipt(SalesOrders salesOrder) {
        if (!traSwitchService.traSwitchOn()) {
            log.info("TRA switch is off. Skipping TRA customer receipt generation for sales order no : {}", salesOrder.getSalesOrderNumber());
            return;
        }

        log.info("Generating TRA customer receipt for sales order no: {}", salesOrder.getSalesOrderNumber());
        TRARequest request = traUtilHelper.convertToTraRequest(salesOrder);
        Optional<TRAResponse> responseOptional = traSubmissionHelper.submitReceipt(request, salesOrder.getSalesOrderNumber());
        if (responseOptional.isEmpty()) {
            return;
        }

        log.info("===========Generating and sending TRA customer receipt for sales order========: {}", salesOrder.getSalesOrderNumber());
        traUtilHelper.generateTraCustomerReceiptAndSend(request, responseOptional.get(), salesOrder);
        log.info("============TRA customer receipt generated and sent for sales order: {}==========", salesOrder.getSalesOrderNumber());
    }

    private Person validateLoggedInUser(String userName) {
        return personRepository.findByEmailAndDeletedIsFalse(userName).orElseThrow(() -> new BadRequestRestApiException()
                .developerMessage("Person with email " + userName + " not found")
                .userMessage("Person not found"));
    }

    private Customers getCustomer(UUID customerPublicId) {
        return customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId).orElseThrow(() -> new BadRequestRestApiException()
                .developerMessage("Customer with public id " + customerPublicId + " not found")
                .userMessage("Customer not found"));
    }

    private OrganizationRegions validateOrganizationRegions(UUID customerPublicId) {
        OrganizationRegions foundRegion = customersRepository.findOrganizationRegionByCustomerPublicId(customerPublicId);

        if (foundRegion != null) {
            return foundRegion;
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer's region not found")
                    .userMessage("Customer's region not found");
        }
    }

    private void validateProductIdsAndStockAvailability(Map<UUID, OrderProductInfo> productSkuMap, Warehouse warehouse) {
        productSkuMap.forEach((productId, orderProductInfo) -> {
            Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(productId);
            if (productOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product with publicId " + productId + " not found")
                        .userMessage("Product not found");
            }
            Product product = productOptional.get();
            if (product.getSellPrice() == null || product.getSellPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product with publicId " + productId + " has invalid sell price")
                        .userMessage(String.format("Product %s has no sell price", product.getProductName()));
            }
            stockUpdateHelper.checkIfProductIsAvailable(product, orderProductInfo.getQuantity(), warehouse);
        });
    }

    private Warehouse getWarehouseByRegion(OrganizationRegions organizationRegion) {
        Warehouse warehouse = warehouseGetterHelper.getWarehouse(organizationRegion);

        if (warehouse == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse not found")
                    .userMessage("Warehouse not found");
        } else {
            return warehouse;
        }
    }

    private Map<UUID, OrderProductInfo> gatherProductItems(final Set<WalkInSalesOrdersRequest.OrderItemsRequest> orderItemsRequests) {
        Map<UUID, OrderProductInfo> productSkuMap = new HashMap<>();
        orderItemsRequests.forEach(item -> {
            productSkuMap.compute(item.productId(), (publicId, info) -> {
                if (info == null) {
                    info = new OrderProductInfo();
                }

                info.setQuantity(info.getQuantity() + item.quantity());
                info.setHasDiscount(item.specialDiscount().compareTo(BigDecimal.ZERO) > 0 || item.volumeDiscount().compareTo(BigDecimal.ZERO) > 0);
                info.setSpecialDiscount(item.specialDiscount());
                info.setVolumeDiscount(item.volumeDiscount());
                return info;
            });
        });
        return productSkuMap;
    }

    private void createWalkInCashReconciliation(ProductRecon productRecon) {

        Reconciliation reconciliation = reconciliationRepository
                .findByProductReconPublicIdAndDeletedIsFalse(productRecon.getPublicId().toString())
                .orElse(new Reconciliation());

        List<ProductItem> productItems = productRecon.getProductItems();

        BigDecimal totalSold = BigDecimal.ZERO;
        BigDecimal missingAmount = BigDecimal.ZERO;
        BigDecimal totalPicked = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (ProductItem productItem : productItems) {
            if (productItem != null && productItem.getProductSellingPrice() != null) {
                totalSold = totalSold.add(productItem.getProductSellingPrice().multiply(BigDecimal.valueOf(productItem.getQuantitySold())));
                missingAmount = missingAmount.add(productItem
                        .getProductSellingPrice()
                        .multiply(BigDecimal.valueOf(productItem.getQuantityMissing())));
                totalPicked = totalPicked.add(productItem
                        .getProductSellingPrice()
                        .multiply(BigDecimal.valueOf(productItem.getQuantityPicked())));
                totalDiscount = totalDiscount.add(productItem.getDiscount());
            }
        }

        reconciliation.setWalkIn(true);
        reconciliation.setDeliveryStatus("Delivered");
        reconciliation.setWarehouseHouseStatus(productRecon.getStatus().name());
        reconciliation.setDeliveryAmount(totalPicked);
        reconciliation.setCreatedBy(productRecon.getCreatedBy());
        reconciliation.setCreationTime(reconciliation.getCreationTime());
        reconciliation.setProductReconPublicId(productRecon.getPublicId().toString());
        reconciliation.setTotalAmountPicked(totalPicked);
        reconciliation.setTotalDiscount(totalDiscount);
        reconciliation.setDepositAmount(totalPicked);
        reconciliation.setReconciled(false);

        reconciliationRepository.save(reconciliation);
    }

    @Override
    public SalesOrders createVanSalesOrder(SalesOrdersRequest salesOrdersRequest, RoutePlanning routePlanning) {
        BigDecimal creditLimitBalance = BigDecimal.ZERO;
        customerCheckerHelper.checkIfCustomerHasActiveOrder(salesOrdersRequest.getCustomerId());

        if (salesOrdersRequest.getBuyOnCredit() == Boolean.TRUE) {
            creditLimitBalance = customerCheckerHelper.checkIfCustomerIsAllowedToBuyOnCredit(salesOrdersRequest.getCustomerId(), salesOrdersRequest.isOrderBookedByParent());
        }

        Person person = routePlanning.getCreatedBy();

        // Validate customer

        Optional<Customers> customersOptional = customersRepository.findByPublicIdAndDeletedIsFalse(salesOrdersRequest.getCustomerId());
        if (customersOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with public ID " + salesOrdersRequest.getCustomerId() + " not found")
                    .userMessage("Customer not found");
        }

        Customers customer = customersOptional.get();

        if (customer.getStatus() == CustomerStatus.De_Activated) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer is De_Activated")
                    .userMessage("Sorry!. Customer is De_Activated");
        }

        boolean hasValidTin = customerCheckerHelper.validateCustomerTinNumber(customer);

        Set<SalesOrderItemsRequest> salesOrderItems = salesOrdersRequest.getSalesOrderItems();

        Map<UUID, Integer> productSkuMap = new HashMap<>();

        // add unique product SKUs and their quantities
        salesOrderItems.forEach(item -> {
            if (item.getProductId() == null || item.getQuantity() <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("ProductId and Quantity must be provided and Quantity must be greater than zero")
                        .userMessage("ProductId and Quantity must be provided and Quantity must be greater than zero");
            }
        });
        salesOrderItems.forEach(item -> {
            if (productSkuMap.containsKey(item.getProductId())) {
                productSkuMap.put(item.getProductId(), productSkuMap.get(item.getProductId()) + item.getQuantity());
            } else {
                productSkuMap.put(item.getProductId(), item.getQuantity());
            }
        });

        OrganizationRegions organizationRegion = customersRepository.findOrganizationRegionByCustomerPublicId(customer.getPublicId());

        if (organizationRegion == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with public ID " + customer.getPublicId() + " not found")
                    .userMessage("Customer's organization region not found");
        }
        Warehouse warehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(routePlanning.getWarehouseId())).orElse(null);

        if (warehouse == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse not found for customer with public ID " + customer.getPublicId())
                    .userMessage("No warehouse found associated to any warehouse region");
        }

        SalesOrders salesOrder = new SalesOrders();
        salesOrder.setOrganizationRegions(organizationRegion);
        salesOrder.setWarehouse(warehouse);
        salesOrder.setSourceChannel(SourceChannelEnum.DELIVERY_AGENT_APP);
        salesOrder.setBookedBy(salesOrdersRequest.getBookedBy());
        salesOrder.setCustomer(customer);
        salesOrder.setSalesOrderNumber("ORD" + counterService.getNextCounter(CounterType.SALES_ORDER));
        salesOrder.setExpectedDeliveryDate(LocalDate.now().plusDays(3));
        salesOrder.setPaymentStatus(PaymentStatusEnum.PENDING);
        salesOrder.setSalesOrderStatus(LoadingStatusEnum.IN_TRANSIT);  // already in DA
        salesOrder.setSalesOrderItems(new HashSet<>());
        salesOrder.setCreatedBy(person);
        salesOrder.setBookedBySalesAgent(salesOrder.isBookedBySalesAgent());
        salesOrder.setCreationTime(LocalDateTime.now());
        salesOrder.setOrganizationRegions(organizationRegion);
        salesOrder.setCreditApplied(salesOrdersRequest.getBuyOnCredit());
        salesOrder.setHasValidTin(hasValidTin);
        salesOrder.setRoutePublicId(routePlanning.getPublicId().toString());
        salesOrder.setDeliveryAgent(deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(routePlanning.getDeliveryAgentId())).orElse(null));

        SalesOrders saved = salesOrdersRepository.save(salesOrder);

        productSkuMap.forEach((productId, quantity) -> {
            Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(productId);

            if (productOptional.isPresent()) {
                Product product = productOptional.get();

                BigDecimal taxAmount = Optional.ofNullable(product.getTaxAmount()).orElse(BigDecimal.ZERO);
                BigDecimal sellPrice = Optional.ofNullable(product.getSellPrice()).orElse(BigDecimal.ZERO);


                // this amount is subject to change if customer delivered quantity becomes less!
                BigDecimal taxCodeAmount = taxAmount.multiply(BigDecimal.valueOf(quantity));
                BigDecimal totalAmount = sellPrice.multiply(BigDecimal.valueOf(quantity));

                BigDecimal taxPercentage = product.getTaxCode() != null ? product.getTaxCode().getCodeValue() : BigDecimal.ZERO;

                SalesOrderItems salesOrderItem = SalesOrderItems.builder()
                        .productId(productId)
                        .quantity(quantity)
                        .pricePerUnit(sellPrice)
                        .status(OrderItemStatus.NotDelivered)
                        .totalAmount(totalAmount)
                        .product(product)
                        .taxAmountValue(taxCodeAmount)
                        .productBeforePriceAfterTax(product.getSellingPriceBeforeTax())
                        .taxPercentageApplied(taxPercentage)
                        .taxApplied(taxPercentage.compareTo(BigDecimal.ZERO) > 0)
                        .build();

                salesOrderItem.setSalesOrder(saved);
                salesOrderItem.setSalesOrderPublicId(saved.getPublicId());
                salesOrderItem.setSalesAgentPublicId(salesOrder.getSalesAgentPublicId());

                SalesOrderItems savedSalesOrderItem = salesOrderItemsRepository.save(salesOrderItem);
                saved.addSalesOrderItem(savedSalesOrderItem);
            }
        });

        saved.setTotalAmount(saved
                .getSalesOrderItems()
                .stream()
                .map(SalesOrderItems::getTotalPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        saved.calculateTotalTax();

        BigDecimal reduceAmount = saved.getSalesOrderItems().stream()
                .map(item -> Optional.ofNullable(item.getTaxAmountValue()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        saved.setTaxAmountValue(reduceAmount);

        saved.setHasSpecialDiscount(false);

        saved.setExpectedDeliveryDate(dayDateCheckerHelper.deliveryDateCalculator(LocalDate.now()));
        SalesOrders savedOrder = salesOrdersRepository.save(saved);

        Map<Product, Integer> reservedStockMap = new HashMap<>();
        savedOrder.getSalesOrderItems()
                .forEach(salesOrderItem -> {
                    Product product = salesOrderItem.getProduct();
                    Integer quantity = salesOrderItem.getQuantity();
                    reservedStockMap.put(product, quantity);
                });

        log.info("checking volume discount=========");
        discountHelper.addVolumeDiscount(savedOrder);


        SalesOrders finalSavedOrder = salesOrdersRepository.findByPublicId(salesOrder.getPublicId());

        BigDecimal amountPayable = finalSavedOrder.getTotalAmountPayable();

        if (finalSavedOrder.isCreditApplied() && creditLimitBalance.compareTo(amountPayable) >= 0) {
            bnplHelper.createUpdateCreditItems(finalSavedOrder, person);
            finalSavedOrder.setOrderType(OrderType.CREDIT);
        } else {
            finalSavedOrder.setOrderType(OrderType.CASH);
            finalSavedOrder.setCreditApplied(false);
        }

        String description = String.format("Sales order %s created with total amount %.2f, order type %s, credit applied %s, has valid Tin %s, has special discount %s",
                finalSavedOrder.getSalesOrderNumber(),
                finalSavedOrder.getTotalAmountPayable(),
                finalSavedOrder.getOrderType(),
                finalSavedOrder.isCreditApplied(),
                finalSavedOrder.isHasValidTin(),
                finalSavedOrder.isHasSpecialDiscount());

        eventsRepository.save(new Events(saved.getPublicId().toString(),
                "Order Created",
                LocalDateTime.now(),
                description,
                "CREATED",
                person));

        return salesOrdersRepository.save(finalSavedOrder);

    }

    @Override
    public List<String> getSalesOrderNumbers(UUID deliveryAgentPublicId, Principal principal) {
        DeliveryAgents deliveryAgents = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(deliveryAgentPublicId).orElse(null);
        if (deliveryAgents == null) {
            return new ArrayList<>();
        }
        return salesOrdersRepository.findByDeliveryAgent(deliveryAgents);
    }

    @Override
    public CustomerReturnOrderItem getSalesOrderItemList(String orderNumber, Principal principal) {
        SalesOrders salesOrders = salesOrdersRepository
                .findBySalesOrderNumberAndDeletedIsFalse(orderNumber)
                .orElse(null);

        if (salesOrders == null) {
            return null;
        }

        return buildCustomerReturnOrderItem(salesOrders);
    }

    private CustomerReturnOrderItem buildCustomerReturnOrderItem(SalesOrders salesOrders) {

        List<SalesOrderItems> salesOrderItems = new ArrayList<>(salesOrders.getSalesOrderItems());

        return CustomerReturnOrderItem.builder()
                .customerName(salesOrders.getCustomer().getName())
                .phone(salesOrders.getCustomer() != null ? salesOrders.getCustomer().getPhoneNumber() : "--")
                .district(salesOrders.getCustomer() != null ? salesOrders.getCustomer().getOrganizationRegions().getName() : "--")
                .ward(salesOrders.getCustomer() != null ? salesOrders.getCustomer().getOrganizationLocation().getName() : "--")
                .storeName(salesOrders.getCustomer().getStoreName())
                .customerType(salesOrders.getCustomer().getCustomerType() != null ? salesOrders.getCustomer().getCustomerType().name() : "--")
                .orderTotal(salesOrders.getTotalAmount() != null ? salesOrders.getTotalAmount().toString() : "--")
                .orderDate(DateFormatter.formatDate(salesOrders.getCreationTime()))
                .status(salesOrders.getStatus().name())
                .deliveryDate(DateFormatter.formatDate(salesOrders.getDeliveryDate()))
                .orderPublicId(salesOrders.getPublicId())
                .salesOrderItemsList(salesOrderItems)
                .build();
    }

    @Override
    public Page<CustomerReturn> getCustomerReturnList(String search, String orderNumber, UUID deliveryAgentPublicId, CustomerReturnStatus status, LocalDate startDate, LocalDate endDate, Principal principal, Pageable pageable) {
        Specification<CustomerReturn> spec = Specification
                .where(CustomerReturnSpecification.isNotDeleted())
                .and(CustomerReturnSpecification.hasDeliveryAgent(deliveryAgentPublicId))
                .and(CustomerReturnSpecification.hasStatus(status))
                .and(CustomerReturnSpecification.hasSearchTerm(search))
                .and(CustomerReturnSpecification.hasDateRange(startDate, endDate));

        return customerReturnRepository.findAll(spec, pageable);
    }

    @Override
    public Page<CustomerReturnItem> getCustomerReturnItemList(UUID returnPublicId, Principal principal, Pageable pageable) {
        CustomerReturn customerReturn = getCustomerReturn(returnPublicId);

        List<CustomerReturnItem> customerReturnItems = customerReturn.getCustomerReturnItems();

        if (customerReturnItems.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), customerReturnItems.size());

        List<CustomerReturnItem> pagedList = customerReturnItems.subList(start, end);

        return new PageImpl<>(pagedList, pageable, customerReturnItems.size());
    }

    @Override
    public ByteArrayOutputStream runExportCustomerReturns(String searchParam, UUID deliveryAgentPublicId, CustomerReturnStatus status, LocalDate startDate, LocalDate endDate, StatementExportType exportType, Pageable pageable, Principal principal) {
        Page<CustomerReturn> customerReturnList = getCustomerReturnList(searchParam, null, deliveryAgentPublicId, status, startDate, endDate, principal, pageable);
        if (exportType == StatementExportType.EXCEL) {
            return customerReturnExport.createExcel(customerReturnList, principal);
        } else {
            return customerReturnExport.generateLeadPdf(customerReturnList, principal);
        }
    }

    @Override
    public SalesAgentOrderPageable getAllSalesOrdersForSA(String searchParam, String salesOrderNumber, LocalDate startDate, LocalDate endDate, Pageable pageable, String salesAgentPublicId) {

        SalesAgents salesAgents = salesAgentsRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(salesAgentPublicId)).orElseThrow(
                () -> new BadRequestRestApiException()
                .developerMessage("Sales agent not found")
                .userMessage("Sales agent not found"));

        Page<SalesOrders> salesOrders = salesOrdersRepository.findAllBySalesAgentPublicIdAndDeletedIsFalse(salesAgentPublicId, pageable);
        List<SalesOrders> filteredOrders = salesOrders.getContent().stream()
                .filter(order -> searchParam == null ||
                        order.getSalesOrderNumber().contains(searchParam) ||
                        order.getCustomer().getPhoneNumber().contains(searchParam))
                .filter(order -> salesOrderNumber == null ||
                        order.getSalesOrderNumber().equals(salesOrderNumber))
                .filter(order -> startDate == null ||
                        order.getCreationTime().isAfter(startDate.minusDays(1).atStartOfDay()))
                .filter(order -> endDate == null ||
                        order.getCreationTime().isBefore(endDate.plusDays(1).atStartOfDay()))
                .collect(Collectors.toList());

        Page<SalesOrders> filteredPage = new PageImpl<>(filteredOrders, pageable, filteredOrders.size());

        SalesAgentOrderPageable salesAgentOrderPageable = new SalesAgentOrderPageable();

        List<SalesOrderResponse> salesOrderResponses = filteredPage.getContent().stream()
                .map(order -> {
                    SalesOrderResponse response = new SalesOrderResponse();
                    response.setPublicId(order.getPublicId());
                    response.setOrderNumber(order.getSalesOrderNumber());
                    response.setCustomer(order.getCustomer().getName());
                    response.setCustomerPublicId(order.getCustomer().getPublicId().toString());
                    response.setTotalAmount(order.getTotalAmountPayable());
                    response.setStatus(order.getSalesOrderStatus().name());
                    response.setCreationDate(DateFormatter.formatDate(order.getCreationTime()));
                    return response;
                })
                .toList();

        BigDecimal totalValue = salesOrderResponses.stream()
                .map(SalesOrderResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        salesAgentOrderPageable.setTotalAmount(totalValue);
        salesAgentOrderPageable.setTotalElements(filteredPage.getTotalElements());
        salesAgentOrderPageable.setPageSize(filteredPage.getSize());
        salesAgentOrderPageable.setTotalPages(filteredPage.getTotalPages());
        salesAgentOrderPageable.setLast(filteredPage.isLast());
        salesAgentOrderPageable.setFirst(filteredPage.isFirst());
        salesAgentOrderPageable.setPageNumber(filteredPage.getNumber());
        salesAgentOrderPageable.setContent(salesOrderResponses);
        salesAgentOrderPageable.setAgent(salesAgents.getName());
        salesAgentOrderPageable.setFrom(startDate != null ? DateFormatter.formatDate(startDate.atStartOfDay()) : "N/A");
        salesAgentOrderPageable.setTo(endDate != null ? DateFormatter.formatDate(endDate.plusDays(1).atStartOfDay()) : "N/A");

        return salesAgentOrderPageable;
    }

    @Override
    public BNPLOrderSummary getBNPLOrderSummary(UUID salesOrderPublicId) {
        SalesOrders salesOrder = getSalesOrder(salesOrderPublicId);

        if (salesOrder.getOrderType() != OrderType.CREDIT) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales order is not a BNPL order")
                    .userMessage("Sales order is not a BNPL order");
        }

        CreditLimitItem creditLimitItem = creditLimitItemRepository.findByOrderNumberAndDeletedIsFalse(salesOrder.getSalesOrderNumber()).orElseThrow(
                () -> new BadRequestRestApiException()
                        .developerMessage("Credit limit item not found for sales order")
                        .userMessage("Credit limit item not found for sales order"));

        BigDecimal discount = salesOrder.getTotalDiscount();

        List<BNPLProducts> bnplProducts = salesOrder.getSalesOrderItems().stream()
                .map(item -> {
                    BNPLProducts bnplProduct = new BNPLProducts();
                    bnplProduct.setSalesOrderItemPublicId(item.getPublicId().toString());
                    bnplProduct.setAmount(item.getTotalPayable().multiply(creditLimitItem.getInterestRate().add(BigDecimal.ONE)));
                    bnplProduct.setSku(item.getProduct().getSku());
                    bnplProduct.setProductName(item.getProduct().getProductName());
                    return bnplProduct;
                })
                .toList();

        BigDecimal totalAmount = bnplProducts.stream()
                .map(BNPLProducts::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BNPLOrderSummary bnplOrderSummary = new BNPLOrderSummary();
        bnplOrderSummary.setProductsList(bnplProducts);
        bnplOrderSummary.setDiscount(discount);
        bnplOrderSummary.setTotal(totalAmount);

        return bnplOrderSummary;
    }

    private CustomerReturn getCustomerReturn(UUID publicId) {
        CustomerReturn customerReturn = customerReturnRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (customerReturn == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer return record not found")
                    .userMessage("Sorry, customer return record not found");
        }
        return customerReturn;
    }
}
